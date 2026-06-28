package com.zhihuitong.security.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class LoginSession implements Serializable {
    private String sessionId;
    private Long userId;
    private Long deptId;
    private Long postId;
    private Long roleId;
    private String username;
    private String nickname;
    private String roleKey;
    private Set<String> permissions = new LinkedHashSet<>();
    private Set<String> menuKeys = new LinkedHashSet<>();
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginTime;
    private LocalDateTime lastAccessTime;
    private long expiresAtEpochMillis;
}
