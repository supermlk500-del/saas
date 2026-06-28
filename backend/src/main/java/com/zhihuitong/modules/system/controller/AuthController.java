package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.system.dto.ChangePasswordRequest;
import com.zhihuitong.modules.system.dto.LoginRequest;
import com.zhihuitong.security.service.AuthService;
import com.zhihuitong.security.service.CaptchaService;
import com.zhihuitong.security.service.JwtTokenService;
import com.zhihuitong.security.service.LoginSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final CaptchaService captchaService;
    private final AuthService authService;
    private final JwtTokenService tokenService;
    private final LoginSessionService sessionService;

    public AuthController(CaptchaService captchaService, AuthService authService,
                          JwtTokenService tokenService, LoginSessionService sessionService) {
        this.captchaService = captchaService;
        this.authService = authService;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
    }

    @GetMapping("/captcha")
    public AjaxResult captcha() {
        return AjaxResult.success(captchaService.createCaptcha());
    }

    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return AjaxResult.success(authService.login(request, servletRequest));
    }

    @GetMapping("/me")
    public AjaxResult me() {
        return AjaxResult.success(authService.currentUser());
    }

    @PutMapping("/password")
    public AjaxResult changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return AjaxResult.success();
    }
    @PostMapping("/logout")
    public AjaxResult logout(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                sessionService.delete(tokenService.parseSessionId(authorization.substring(7)));
            } catch (RuntimeException ignored) {
                // Logout remains idempotent for expired tokens.
            }
        }
        return AjaxResult.success();
    }
}
