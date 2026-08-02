package com.zhihuitong.security.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.system.dto.ChangePasswordRequest;
import com.zhihuitong.modules.system.dto.LoginRequest;
import com.zhihuitong.modules.system.entity.SysLoginLog;
import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.mapper.SysLoginLogMapper;
import com.zhihuitong.modules.system.mapper.SysUserMapper;
import com.zhihuitong.modules.system.vo.LoginTokenVo;
import com.zhihuitong.security.config.SecurityProperties;
import com.zhihuitong.security.model.LoginPrincipal;
import com.zhihuitong.security.model.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final LoginSessionService sessionService;
    private final JwtTokenService tokenService;
    private final SecurityProperties properties;
    private final PermissionService permissionService;
    private final SysUserMapper userMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, CaptchaService captchaService,
                       LoginSessionService sessionService, JwtTokenService tokenService,
                       SecurityProperties properties, PermissionService permissionService,
                       SysUserMapper userMapper, SysLoginLogMapper loginLogMapper,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.captchaService = captchaService;
        this.sessionService = sessionService;
        this.tokenService = tokenService;
        this.properties = properties;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginTokenVo login(LoginRequest request, HttpServletRequest servletRequest) {
        String username = request.getUsername().trim();
        String ipAddress = resolveIp(servletRequest);
        try {
            captchaService.validateAndConsume(request.getCaptchaUuid(), request.getCaptchaCode());
        } catch (BusinessException exception) {
            recordLogin(null, username, null, ipAddress, servletRequest.getHeader("User-Agent"), "1", exception.getMessage());
            throw exception;
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (AuthenticationException exception) {
            recordLogin(null, username, null, ipAddress, servletRequest.getHeader("User-Agent"), "1", "用户名或密码错误");
            throw new BusinessException(401, "用户名或密码错误");
        }

        LoginPrincipal principal = (LoginPrincipal) authentication.getPrincipal();
        LoginSession session = new LoginSession();
        session.setUserId(principal.getUserId());
        session.setDeptId(principal.getDeptId());
        session.setPostId(principal.getPostId());
        session.setRoleId(principal.getRoleId());
        session.setUsername(principal.getUsername());
        session.setNickname(principal.getNickname());
        session.setRoleKey(principal.getRoleKey());
        session.setPermissions(principal.getPermissions());
        session.setMenuKeys(principal.getMenuKeys());
        session.setIpAddress(ipAddress);
        session.setUserAgent(servletRequest.getHeader("User-Agent"));
        sessionService.create(session);
        updateLoginInfo(principal.getUserId(), session.getIpAddress());
        recordLogin(principal.getUserId(), principal.getUsername(), session.getSessionId(), ipAddress,
                session.getUserAgent(), "0", "登录成功");
        return new LoginTokenVo(tokenService.createToken(session.getSessionId(), session.getUsername()),
                properties.getJwt().getExpireMinutes() * 60);
    }

    public Map<String, Object> currentUser() {
        LoginPrincipal principal = permissionService.currentPrincipal();
        if (principal == null) throw new BusinessException(401, "未登录");
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", principal.getUserId());
        user.put("userName", principal.getUsername());
        user.put("nickName", principal.getNickname());
        user.put("deptId", principal.getDeptId());
        user.put("postId", principal.getPostId());
        user.put("roleId", principal.getRoleId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("roleKey", principal.getRoleKey());
        result.put("permissions", principal.getPermissions());
        result.put("menuKeys", principal.getMenuKeys());
        return result;
    }

    public void changePassword(ChangePasswordRequest request) {
        LoginPrincipal principal = permissionService.currentPrincipal();
        if (principal == null) throw new BusinessException(401, "未登录");
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(400, "当前密码错误");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(400, "新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPwdUpdateTime(LocalDateTime.now());
        user.setUpdatedBy(principal.getUserId());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        sessionService.invalidateUserSessions(user.getUserId());
    }
    private void updateLoginInfo(Long userId, String ip) {
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setLoginIp(ip);
        update.setLoginDate(LocalDateTime.now());
        userMapper.updateById(update);
    }

    private void recordLogin(Long userId, String username, String sessionId, String ipAddress,
                             String userAgent, String status, String message) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUserName(truncate(username, 64));
            loginLog.setSessionId(truncate(sessionId, 64));
            loginLog.setIpaddr(truncate(ipAddress, 64));
            loginLog.setBrowser(truncate(userAgent, 100));
            loginLog.setStatus(truncate(status, 20));
            loginLog.setMessage(truncate(message, 500));
            loginLog.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(loginLog);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist login audit for user {}: {}", username, exception.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
