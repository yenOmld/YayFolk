package com.yayfolk.backend.config;

import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.UserRepository;
import com.yayfolk.backend.utils.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 从请求头中提取 JWT token 并验证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public JwtAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 获取 Authorization header
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7); // 去掉 "Bearer " 前缀
            
            try {
                // 验证 token
                if (JwtUtil.validateToken(token)) {
                    // 从 token 中获取用户名
                    String username = JwtUtil.getUsernameFromToken(token);
                    
                    if (username != null) {
                        User user = userRepository.findByUsername(username).orElse(null);
                        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"账号已被封禁\"}");
                            return;
                        }

                        // 始终在请求范围内提供用户名，避免控制器取不到
                        request.setAttribute("username", username);
                        
                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                        
                        System.out.println("JWT 认证成功 - 用户：" + username);
                    }
                }
            } catch (Exception e) {
                System.err.println("JWT 认证失败：" + e.getMessage());
                // Token 无效或过期，继续执行但不设置认证信息
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
