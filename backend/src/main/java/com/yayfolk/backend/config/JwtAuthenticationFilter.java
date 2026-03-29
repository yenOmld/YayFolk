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
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            try {
                if (JwtUtil.validateToken(token)) {
                    String username = JwtUtil.getUsernameFromToken(token);

                    if (username != null) {
                        User user = userRepository.findByUsername(username).orElse(null);
                        if (user == null) {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\"}");
                            return;
                        }

                        boolean banned = user.getStatus() != null && user.getStatus() == 0;
                        if (banned && !isAllowedForBannedUser(request.getRequestURI())) {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            String reason = user.getBanReason();
                            if (reason != null && !reason.trim().isEmpty()) {
                                String safeReason = reason.replace("\\", "\\\\").replace("\"", "\\\"");
                                response.getWriter().write("{\"code\":403,\"message\":\"账号已被封禁，原因：" + safeReason + "\"}");
                            } else {
                                response.getWriter().write("{\"code\":403,\"message\":\"账号已被封禁\"}");
                            }
                            return;
                        }

                        request.setAttribute("username", username);

                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("JWT authentication failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedForBannedUser(String requestPath) {
        return requestPath != null && requestPath.startsWith("/api/public/");
    }
}
