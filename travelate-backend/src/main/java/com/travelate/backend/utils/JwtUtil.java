package com.travelate.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static final String DEFAULT_SECRET = "please_set_JWT_SECRET_env";
    private static final String SECRET_KEY = getSecret();
    private static final long EXPIRATION_TIME = getExpiration();

    // 生成token
    public static String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 验证token
    public static boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 从token中获取用户名
    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    private static String getSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env == null || env.trim().isEmpty()) {
            return DEFAULT_SECRET;
        }
        return env;
    }

    private static long getExpiration() {
        String env = System.getenv("JWT_EXPIRATION_MS");
        if (env == null || env.trim().isEmpty()) {
            return 3L * 24 * 60 * 60 * 1000;
        }
        try {
            return Long.parseLong(env);
        } catch (NumberFormatException e) {
            return 3L * 24 * 60 * 60 * 1000;
        }
    }
}
