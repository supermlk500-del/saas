package com.zhihuitong.security.service;

import com.zhihuitong.modules.system.dto.LoginRequest;
import com.zhihuitong.modules.system.entity.SysLoginLog;
import com.zhihuitong.modules.system.mapper.SysLoginLogMapper;
import com.zhihuitong.modules.system.mapper.SysUserMapper;
import com.zhihuitong.security.config.SecurityProperties;
import com.zhihuitong.security.model.LoginPrincipal;
import com.zhihuitong.security.model.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CaptchaService captchaService;
    @Mock private LoginSessionService sessionService;
    @Mock private JwtTokenService tokenService;
    @Mock private PermissionService permissionService;
    @Mock private SysUserMapper userMapper;
    @Mock private SysLoginLogMapper loginLogMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Authentication authentication;
    @Mock private HttpServletRequest servletRequest;

    private AuthService service;
    private SecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getJwt().setExpireMinutes(30);
        service = new AuthService(authenticationManager, captchaService, sessionService, tokenService,
                properties, permissionService, userMapper, loginLogMapper, passwordEncoder);
    }

    @Test
    void loginTruncatesLongBrowserBeforeWritingAudit() {
        String userAgent = "Mozilla/5.0 ".repeat(20);
        prepareSuccessfulLogin(userAgent);

        service.login(loginRequest(), servletRequest);

        ArgumentCaptor<SysLoginLog> captor = ArgumentCaptor.forClass(SysLoginLog.class);
        verify(loginLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getBrowser()).hasSize(100);
    }

    @Test
    void auditFailureDoesNotBlockSuccessfulLogin() {
        prepareSuccessfulLogin("Chrome");
        doThrow(new RuntimeException("audit unavailable")).when(loginLogMapper).insert(any(SysLoginLog.class));

        assertThatCode(() -> service.login(loginRequest(), servletRequest)).doesNotThrowAnyException();
    }

    private void prepareSuccessfulLogin(String userAgent) {
        LoginPrincipal principal = new LoginPrincipal(
                1L, 100L, 1L, 1L, "admin", "超级管理员", null,
                "admin", Set.of("*:*:*"), Set.of("*"), true);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getHeader("User-Agent")).thenReturn(userAgent);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenService.createToken(any(), any())).thenReturn("token");
        when(sessionService.create(any(LoginSession.class))).thenAnswer(invocation -> {
            LoginSession session = invocation.getArgument(0);
            session.setSessionId("session-id");
            return session;
        });
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");
        request.setCaptchaUuid("uuid");
        request.setCaptchaCode("ABCD");
        return request;
    }
}
