package com.travelate.backend.controller;

import com.travelate.backend.dto.LoginRequest;
import com.travelate.backend.dto.LoginResponse;
import com.travelate.backend.dto.RegisterRequest;
import com.travelate.backend.dto.ResponseDto;
import com.travelate.backend.service.UserService;
import com.travelate.backend.service.EmailService;
import com.travelate.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.InetSocketAddress;
import java.net.Proxy;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${GITHUB_CLIENT_ID:}")
    private String githubClientId;

    @Value("${GITHUB_CLIENT_SECRET:}")
    private String githubClientSecret;

    @Value("${GITHUB_REDIRECT_URI:http://localhost:8080/api/oauth/github/callback}")
    private String githubRedirectUri;

    @Value("${HTTP_PROXY_HOST:}")
    private String httpProxyHost;

    @Value("${HTTP_PROXY_PORT:}")
    private Integer httpProxyPort;

    public AuthController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
        this.restTemplate = createRestTemplate();
    }

    /**
     * 创建并配置 RestTemplate
     * @return 配置好超时的 RestTemplate
     */
    private RestTemplate createRestTemplate() {
        // 配置连接超时和读取超时（单位：毫秒）
        // 访问 GitHub API 可能需要更长的超时时间
        int connectTimeout = 30000; // 连接超时 30 秒
        int readTimeout = 60000;    // 读取超时 60 秒
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        // 如果在中国大陆访问 GitHub API 遇到连接超时，可以配置 HTTP 代理
        // 示例：使用系统代理或自定义代理
        // 方法 1: 使用系统代理属性（需要在 JVM 启动参数中设置 -Dhttp.proxyHost=xxx -Dhttp.proxyPort=xxx）
        // Proxy proxy = Proxy.NO_PROXY; // 默认不使用代理
        // 如果需要启用代理，取消下面代码的注释并修改代理地址
        // proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("your-proxy-host", your-proxy-port));
        // factory.setProxy(proxy);
        
        // 方法 2: 使用配置文件中的代理设置
        if (httpProxyHost != null && !httpProxyHost.isEmpty() && httpProxyPort != null) {
            System.out.println("使用HTTP代理: " + httpProxyHost + ":" + httpProxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
            factory.setProxy(proxy);
        }
        
        return new RestTemplate(factory);
    }

    @PostMapping("/register")
public ResponseDto register(@Valid @RequestBody RegisterRequest request) {
    try {
        // 验证密码
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseDto.error(400, "两次输入的密码不一致");
        }

        // 解析用户名是手机号还是邮箱
        String username = request.getUsername();
        String phone = null;
        String email = null;

        if (username.matches("^\\+?[1-9]\\d{1,14}$")) {
            phone = username;
        } else if (username.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            email = username;
        } else {
            return ResponseDto.error(400, "请输入有效的手机号或邮箱");
        }

        // 验证验证码
        if (!emailService.verifyCode(username, request.getVerificationCode())) {
            return ResponseDto.error(400, "验证码无效或已过期");
        }

        // 注册用户
        com.travelate.backend.entity.User user = userService.register(username, request.getPassword(), request.getNickname(), phone, email, request.getCountry(), request.getLangCode(), request.getRegionCode());
        return ResponseDto.success(user);
    } catch (RuntimeException e) {
        return ResponseDto.error(400, e.getMessage());
    }
}

    @PostMapping("/login")
    public ResponseDto login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            com.travelate.backend.entity.User user = userService.login(request.getUsername(), request.getPassword());
            
            // 检测IP并更新地区信息
            try {
                String clientIp = getClientIp(httpRequest);
                Map<String, Object> geoInfo = getGeoInfoByIp(clientIp);
                if (geoInfo != null) {
                    if (geoInfo.get("country_name") != null) {
                        user.setCountry((String) geoInfo.get("country_name"));
                    }
                    if (geoInfo.get("country_code") != null) {
                        user.setRegionCode((String) geoInfo.get("country_code"));
                    }
                    userService.save(user);
                }
            } catch (Exception e) {
                // IP检测失败不影响登录
                System.err.println("IP检测失败: " + e.getMessage());
            }
            
            // 生成JWT token
            String token = JwtUtil.generateToken(user.getUsername());
            // 返回token和用户信息
            LoginResponse loginResponse = new LoginResponse(token, user);
            return ResponseDto.success(loginResponse);
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGeoInfoByIp(String ip) {
        try {
            // 跳过本地IP
            if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                return null;
            }
            
            // 尝试第一个API: ipapi.co
            try {
                String url = "https://ipapi.co/" + ip + "/json/";
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                if (result != null && !result.containsKey("error")) {
                    return result;
                }
            } catch (Exception e) {
                // 尝试备用API
            }
            
            // 备用API: ip-api.com
            try {
                String url = "http://ip-api.com/json/" + ip;
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                if (result != null && "success".equals(result.get("status"))) {
                    // 转换字段名以兼容
                    Map<String, Object> converted = new HashMap<>();
                    converted.put("country_name", result.get("country"));
                    converted.put("country_code", result.get("countryCode"));
                    return converted;
                }
            } catch (Exception e) {
                // API调用失败
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/login/code")
    public ResponseDto loginByCode(@RequestBody LoginByCodeRequest request, HttpServletRequest httpRequest) {
        try {
            String username = request.getUsername();
            String verificationCode = request.getVerificationCode();
            
            if (!emailService.verifyCode(username, verificationCode)) {
                return ResponseDto.error(400, "验证码无效或已过期");
            }
            
            com.travelate.backend.entity.User user = userService.findByUsername(username);
            
            // 检测IP并更新地区信息
            try {
                String clientIp = getClientIp(httpRequest);
                Map<String, Object> geoInfo = getGeoInfoByIp(clientIp);
                if (geoInfo != null) {
                    if (geoInfo.get("country_name") != null) {
                        user.setCountry((String) geoInfo.get("country_name"));
                    }
                    if (geoInfo.get("country_code") != null) {
                        user.setRegionCode((String) geoInfo.get("country_code"));
                    }
                    userService.save(user);
                }
            } catch (Exception e) {
                System.err.println("IP检测失败: " + e.getMessage());
            }
            
            String token = JwtUtil.generateToken(user.getUsername());
            LoginResponse loginResponse = new LoginResponse(token, user);
            return ResponseDto.success(loginResponse);
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
    
    private static class LoginByCodeRequest {
        private String username;
        private String verificationCode;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getVerificationCode() {
            return verificationCode;
        }
        
        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }
    }

    @PostMapping("/send-code")
    public ResponseDto sendCode(@RequestBody SendCodeRequest request) {
        try {
            String username = request.getUsername();
            if (!username.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return ResponseDto.error(400, "请输入有效的邮箱地址");
            }
            
            emailService.sendVerificationCode(username);
            return ResponseDto.success("验证码已发送");
        } catch (Exception e) {
            return ResponseDto.error(500, "发送验证码失败: " + e.getMessage());
        }
    }
    
    private static class SendCodeRequest {
        private String username;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }

    @PostMapping("/reset-password")
    public ResponseDto resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            String username = request.getUsername();
            String verificationCode = request.getVerificationCode();
            String newPassword = request.getNewPassword();
            String confirmPassword = request.getConfirmPassword();
            
            // 验证两次密码是否一致
            if (!newPassword.equals(confirmPassword)) {
                return ResponseDto.error(400, "两次输入的密码不一致");
            }
            
            // 验证验证码
            if (!emailService.verifyCode(username, verificationCode)) {
                return ResponseDto.error(400, "验证码无效或已过期");
            }
            
            // 重置密码
            userService.resetPassword(username, newPassword);
            return ResponseDto.success("密码重置成功");
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }
    
    private static class ResetPasswordRequest {
        private String username;
        private String verificationCode;
        private String newPassword;
        private String confirmPassword;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getVerificationCode() {
            return verificationCode;
        }
        
        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }
        
        public String getNewPassword() {
            return newPassword;
        }
        
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
        
        public String getConfirmPassword() {
            return confirmPassword;
        }
        
        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }

    // GitHub OAuth登录
    @GetMapping("/oauth/github/login")
    public RedirectView githubLogin() {
        String state = UUID.randomUUID().toString();
        String githubAuthUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=" + githubClientId +
                "&redirect_uri=" + githubRedirectUri +
                "&scope=user:email,read:user" +
                "&state=" + state;
        return new RedirectView(githubAuthUrl);
    }

    // GitHub OAuth 回调
    @GetMapping("/oauth/github/callback")
    public RedirectView githubCallback(@RequestParam("code") String code, @RequestParam("state") String state, HttpServletRequest httpRequest) {
        try {
            System.out.println("=== GitHub OAuth 回调开始 ===");
            System.out.println("授权码: " + code);
            System.out.println("State: " + state);
            
            // 1. 用授权码获取 access token
            String tokenUrl = "https://github.com/login/oauth/access_token";
            Map<String, String> tokenParams = new java.util.HashMap<>();
            tokenParams.put("client_id", githubClientId);
            tokenParams.put("client_secret", githubClientSecret);
            tokenParams.put("code", code);
            tokenParams.put("redirect_uri", githubRedirectUri);
            tokenParams.put("state", state);
            
            System.out.println("请求 GitHub access token...");
            System.out.println("Client ID: " + githubClientId);
            System.out.println("Redirect URI: " + githubRedirectUri);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = (Map<String, Object>) restTemplate.postForObject(tokenUrl, tokenParams, Map.class);
            
            System.out.println("GitHub token 响应: " + tokenResponse);
            
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                System.err.println("GitHub OAuth 错误：无法获取 access token，响应：" + tokenResponse);
                String errorDesc = tokenResponse != null ? (String) tokenResponse.get("error_description") : "未知错误";
                return new RedirectView("http://localhost:5173?error=github_token_failed&desc=" + java.net.URLEncoder.encode(errorDesc, "UTF-8"));
            }
            
            String accessToken = (String) tokenResponse.get("access_token");
            System.out.println("成功获取 GitHub access token");

            // 2. 用 access token 获取用户信息
            String userUrl = "https://api.github.com/user";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "token " + accessToken);
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            System.out.println("请求 GitHub 用户信息...");
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) restTemplate.exchange(userUrl, org.springframework.http.HttpMethod.GET, entity, Map.class).getBody();
            
            System.out.println("GitHub 用户信息: " + userInfo);

            // 3. 处理用户信息，创建或更新用户
            String githubId = String.valueOf(userInfo.get("id"));
            String username = (String) userInfo.get("login");
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String avatarUrl = (String) userInfo.get("avatar_url");

            System.out.println("GitHub ID: " + githubId);
            System.out.println("Username: " + username);
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);

            // 获取邮箱（如果用户信息中没有，通过 GitHub API 获取）
            if (email == null) {
                System.out.println("用户信息中没有邮箱，尝试获取邮箱列表...");
                String emailsUrl = "https://api.github.com/user/emails";
                Map<String, Object>[] emails = restTemplate.exchange(emailsUrl, org.springframework.http.HttpMethod.GET, entity, Map[].class).getBody();
                if (emails != null && emails.length > 0) {
                    for (@SuppressWarnings("unchecked") Map<String, Object> emailObj : emails) {
                        if (Boolean.TRUE.equals(emailObj.get("primary"))) {
                            email = (String) emailObj.get("email");
                            System.out.println("找到主邮箱: " + email);
                            break;
                        }
                    }
                }
            }

            // 创建或更新用户
            System.out.println("创建或更新用户...");
            com.travelate.backend.entity.User user = userService.createOrUpdateUserFromGithub(githubId, username, email, name, avatarUrl);
            
            // 检测IP并更新地区信息
            try {
                String clientIp = getClientIp(httpRequest);
                Map<String, Object> geoInfo = getGeoInfoByIp(clientIp);
                if (geoInfo != null) {
                    if (geoInfo.get("country_name") != null) {
                        user.setCountry((String) geoInfo.get("country_name"));
                    }
                    if (geoInfo.get("country_code") != null) {
                        user.setRegionCode((String) geoInfo.get("country_code"));
                    }
                    userService.save(user);
                }
            } catch (Exception e) {
                System.err.println("IP检测失败: " + e.getMessage());
            }

            // 生成JWT token
            String token = JwtUtil.generateToken(user.getUsername());

            // 重定向到前端，携带 token 和用户信息
            ObjectMapper objectMapper = new ObjectMapper();
            String userJson = objectMapper.writeValueAsString(user);
            String encodedUserJson = java.net.URLEncoder.encode(userJson, "UTF-8");
            System.out.println("=== GitHub 登录成功 ===");
            System.out.println("用户：" + username);
            return new RedirectView("http://localhost:5173?token=" + token + "&user=" + encodedUserJson);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("GitHub OAuth HTTP 错误：" + e.getStatusCode() + " - " + e.getMessage());
            System.err.println("响应体: " + e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                return new RedirectView("http://localhost:5173?error=github_auth_failed");
            } else if (e.getStatusCode().value() == 429) {
                System.err.println("GitHub API 限流，请稍后再试");
                return new RedirectView("http://localhost:5173?error=github_rate_limit");
            }
            return new RedirectView("http://localhost:5173?error=github_login_failed");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("GitHub OAuth 网络错误：" + e.getMessage());
            System.err.println("可能的原因：网络连接问题、代理配置问题或GitHub服务不可用");
            return new RedirectView("http://localhost:5173?error=github_network_error");
        } catch (Exception e) {
            System.err.println("GitHub 登录异常：" + e.getMessage());
            e.printStackTrace();
            return new RedirectView("http://localhost:5173?error=github_login_failed");
        }
    }
}
