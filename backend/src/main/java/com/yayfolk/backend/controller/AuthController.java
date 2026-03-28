package com.yayfolk.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.dto.LoginRequest;
import com.yayfolk.backend.dto.LoginResponse;
import com.yayfolk.backend.dto.RegisterRequest;
import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.service.EmailService;
import com.yayfolk.backend.service.UserService;
import com.yayfolk.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    @Value("${FRONTEND_BASE_URL:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${HTTP_PROXY_HOST:}")
    private String httpProxyHost;

    @Value("${HTTP_PROXY_PORT:}")
    private Integer httpProxyPort;

    public AuthController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);

        if (httpProxyHost != null && !httpProxyHost.isEmpty() && httpProxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
            factory.setProxy(proxy);
        }

        return new RestTemplate(factory);
    }

    @PostMapping("/register")
    public ResponseDto register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseDto.error(400, "Passwords do not match");
            }

            String username = request.getUsername();
            String phone = null;
            String email = null;

            if (username.matches("^\\+?[1-9]\\d{1,14}$")) {
                phone = username;
            } else if (username.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                email = username;
            } else {
                return ResponseDto.error(400, "Please enter a valid phone number or email address");
            }

            if (!emailService.verifyCode(username, request.getVerificationCode())) {
                return ResponseDto.error(400, "Invalid or expired verification code");
            }

            User user = userService.register(
                username,
                request.getPassword(),
                request.getNickname(),
                phone,
                email,
                request.getCountry(),
                request.getLangCode(),
                request.getRegionCode()
            );
            return ResponseDto.success(user);
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseDto login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            enrichUserRegion(user, httpRequest);

            String token = JwtUtil.generateToken(user.getUsername());
            return ResponseDto.success(new LoginResponse(token, user));
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/login/code")
    public ResponseDto loginByCode(@RequestBody LoginByCodeRequest request, HttpServletRequest httpRequest) {
        try {
            String username = request.getUsername();
            String verificationCode = request.getVerificationCode();

            if (!emailService.verifyCode(username, verificationCode)) {
                return ResponseDto.error(400, "Invalid or expired verification code");
            }

            User user = userService.findByUsername(username);
            enrichUserRegion(user, httpRequest);

            String token = JwtUtil.generateToken(user.getUsername());
            return ResponseDto.success(new LoginResponse(token, user));
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @PostMapping("/send-code")
    public ResponseDto sendCode(@RequestBody SendCodeRequest request) {
        try {
            String username = request.getUsername();
            if (!username.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return ResponseDto.error(400, "Please enter a valid email address");
            }

            emailService.sendVerificationCode(username);
            return ResponseDto.success("Verification code sent");
        } catch (Exception e) {
            return ResponseDto.error(500, "Failed to send verification code: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseDto resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseDto.error(400, "Passwords do not match");
            }

            if (!emailService.verifyCode(request.getUsername(), request.getVerificationCode())) {
                return ResponseDto.error(400, "Invalid or expired verification code");
            }

            userService.resetPassword(request.getUsername(), request.getNewPassword());
            return ResponseDto.success("Password reset successfully");
        } catch (RuntimeException e) {
            return ResponseDto.error(400, e.getMessage());
        }
    }

    @GetMapping("/oauth/github/login")
    public RedirectView githubLogin() {
        String state = UUID.randomUUID().toString();
        String githubAuthUrl = "https://github.com/login/oauth/authorize"
            + "?client_id=" + githubClientId
            + "&redirect_uri=" + urlEncode(githubRedirectUri)
            + "&scope=user:email,read:user"
            + "&state=" + state;
        return new RedirectView(githubAuthUrl);
    }

    @GetMapping("/oauth/github/callback")
    public RedirectView githubCallback(
        @RequestParam("code") String code,
        @RequestParam("state") String state,
        HttpServletRequest httpRequest
    ) {
        try {
            String accessToken = fetchGithubAccessToken(code, state);
            Map<String, Object> userInfo = fetchGithubUserInfo(accessToken);

            String githubId = String.valueOf(userInfo.get("id"));
            String username = (String) userInfo.get("login");
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String avatarUrl = (String) userInfo.get("avatar_url");

            if (email == null || email.isEmpty()) {
                email = fetchPrimaryGithubEmail(accessToken);
            }

            User user = userService.createOrUpdateUserFromGithub(githubId, username, email, name, avatarUrl);
            enrichUserRegion(user, httpRequest);

            String token = JwtUtil.generateToken(user.getUsername());
            String userJson = new ObjectMapper().writeValueAsString(user);
            String encodedUserJson = URLEncoder.encode(userJson, StandardCharsets.UTF_8.name());
            return new RedirectView(frontendBaseUrl + "?token=" + token + "&user=" + encodedUserJson);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                return new RedirectView(frontendBaseUrl + "?error=github_auth_failed");
            }
            if (e.getStatusCode().value() == 429) {
                return new RedirectView(frontendBaseUrl + "?error=github_rate_limit");
            }
            return new RedirectView(frontendBaseUrl + "?error=github_login_failed");
        } catch (ResourceAccessException e) {
            return new RedirectView(frontendBaseUrl + "?error=github_network_error");
        } catch (Exception e) {
            return new RedirectView(frontendBaseUrl + "?error=github_login_failed");
        }
    }

    private void enrichUserRegion(User user, HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIp(httpRequest);
            Map<String, Object> geoInfo = getGeoInfoByIp(clientIp);
            if (geoInfo == null) {
                return;
            }

            if (geoInfo.get("country_name") != null) {
                user.setCountry((String) geoInfo.get("country_name"));
            }
            if (geoInfo.get("country_code") != null) {
                user.setRegionCode((String) geoInfo.get("country_code"));
            }
            userService.save(user);
        } catch (Exception ignored) {
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGeoInfoByIp(String ip) {
        try {
            if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")
                || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                return null;
            }

            try {
                String url = "https://ipapi.co/" + ip + "/json/";
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                if (result != null && !result.containsKey("error")) {
                    return result;
                }
            } catch (Exception ignored) {
            }

            try {
                String url = "http://ip-api.com/json/" + ip;
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                if (result != null && "success".equals(result.get("status"))) {
                    Map<String, Object> converted = new HashMap<>();
                    converted.put("country_name", result.get("country"));
                    converted.put("country_code", result.get("countryCode"));
                    return converted;
                }
            } catch (Exception ignored) {
            }

            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchGithubAccessToken(String code, String state) {
        String tokenUrl = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubClientId);
        form.add("client_secret", githubClientSecret);
        form.add("code", code);
        form.add("redirect_uri", githubRedirectUri);
        form.add("state", state);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, Map.class);
        Map<String, Object> tokenResponse = response.getBody();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Failed to get GitHub access token");
        }
        return (String) tokenResponse.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchGithubUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + accessToken);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.github.com/user",
            HttpMethod.GET,
            entity,
            Map.class
        );
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private String fetchPrimaryGithubEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + accessToken);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map[]> response = restTemplate.exchange(
            "https://api.github.com/user/emails",
            HttpMethod.GET,
            entity,
            Map[].class
        );

        Map<String, Object>[] emails = response.getBody();
        if (emails == null) {
            return null;
        }

        for (Map<String, Object> emailObj : emails) {
            if (Boolean.TRUE.equals(emailObj.get("primary"))) {
                return (String) emailObj.get("email");
            }
        }
        return null;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
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

    private static class SendCodeRequest {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
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
}