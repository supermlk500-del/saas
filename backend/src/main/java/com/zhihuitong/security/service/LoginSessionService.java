package com.zhihuitong.security.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.security.config.SecurityProperties;
import com.zhihuitong.security.model.LoginSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LoginSessionService {
    private static final Logger log = LoggerFactory.getLogger(LoginSessionService.class);

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String USER_SESSION_PREFIX = "auth:user-session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityProperties properties;

    public LoginSessionService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                               SecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public LoginSession create(LoginSession session) {
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setLoginTime(LocalDateTime.now());
        session.setLastAccessTime(session.getLoginTime());
        refresh(session);
        String userKey = userSessionKey(session.getUserId());
        redisTemplate.opsForSet().add(userKey, session.getSessionId());
        redisTemplate.expire(userKey, sessionDuration());
        return session;
    }

    public LoginSession get(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        String value = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (value == null) {
            return null;
        }
        try {
            LoginSession session = objectMapper.readValue(value, LoginSession.class);
            long remaining = session.getExpiresAtEpochMillis() - System.currentTimeMillis();
            if (remaining <= properties.getJwt().getRefreshThresholdMinutes() * 60_000) {
                session.setLastAccessTime(LocalDateTime.now());
                refresh(session);
            }
            return session;
        } catch (JsonProcessingException exception) {
            log.warn("Invalid login session payload found, session will be deleted: {}", exception.getMessage());
            delete(sessionId);
            return null;
        }
    }

    public void refresh(LoginSession session) {
        long expiresAt = System.currentTimeMillis() + properties.getJwt().getExpireMinutes() * 60_000;
        session.setExpiresAtEpochMillis(expiresAt);
        try {
            redisTemplate.opsForValue().set(sessionKey(session.getSessionId()), objectMapper.writeValueAsString(session), sessionDuration());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize login session", exception);
        }
    }

    public void delete(String sessionId) {
        LoginSession session = readWithoutRefresh(sessionId);
        redisTemplate.delete(sessionKey(sessionId));
        if (session != null) {
            redisTemplate.opsForSet().remove(userSessionKey(session.getUserId()), sessionId);
        }
    }

    public void invalidateUserSessions(Long userId) {
        String userKey = userSessionKey(userId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(userKey);
        if (sessionIds != null) {
            sessionIds.forEach(id -> redisTemplate.delete(sessionKey(id)));
        }
        redisTemplate.delete(userKey);
    }

    public void invalidateAllSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        Set<String> userKeys = redisTemplate.keys(USER_SESSION_PREFIX + "*");
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
    }
    public List<LoginSession> listOnlineSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
        List<LoginSession> sessions = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                if (value == null) continue;
                try {
                    sessions.add(objectMapper.readValue(value, LoginSession.class));
                } catch (JsonProcessingException exception) {
                    log.warn("Invalid online session payload found, key will be deleted: {}", exception.getMessage());
                    redisTemplate.delete(key);
                }
            }
        }
        return sessions;
    }

    private LoginSession readWithoutRefresh(String sessionId) {
        String value = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, LoginSession.class);
        } catch (JsonProcessingException exception) {
            log.debug("Failed to parse login session without refresh: {}", exception.getMessage());
            return null;
        }
    }

    private Duration sessionDuration() {
        return Duration.ofMinutes(properties.getJwt().getExpireMinutes());
    }

    private String sessionKey(String sessionId) { return SESSION_PREFIX + sessionId; }
    private String userSessionKey(Long userId) { return USER_SESSION_PREFIX + userId; }
}
