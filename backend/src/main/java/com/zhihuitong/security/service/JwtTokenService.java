package com.zhihuitong.security.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.security.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecurityProperties properties;
    private final SecretKey key;

    public JwtTokenService(SecurityProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String createToken(String sessionId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getJwt().getExpireMinutes() * 60)))
                .signWith(key)
                .compact();
    }

    public String parseSessionId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.get("sid", String.class);
        } catch (Exception exception) {
            throw new BusinessException(401, "登录状态无效或已过期");
        }
    }
}
