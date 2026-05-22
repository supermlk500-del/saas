package com.zhihuitong.controller;

import com.zhihuitong.common.domain.AjaxResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
public class AuthController {

    @GetMapping("/captchaImage")
    public AjaxResult captchaImage() {
        String tinyPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8Xw8AApMBgU2YsGQAAAAASUVORK5CYII=";
        return AjaxResult.success(Map.of(
                "captchaEnabled", true,
                "img", tinyPng,
                "uuid", UUID.randomUUID().toString()
        ));
    }

    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginRequest request) {
        return AjaxResult.success(Map.of(
                "token", "dev-token-" + request.username().trim()
        ));
    }

    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        return AjaxResult.success(Map.of(
                "user", Map.of(
                        "userName", "admin",
                        "nickName", "Administrator"
                ),
                "roles", List.of("admin"),
                "permissions", List.of("*:*:*")
        ));
    }

    @PostMapping("/logout")
    public AjaxResult logout() {
        return AjaxResult.success("Logout success");
    }

    @PostMapping("/register")
    public AjaxResult register(@Valid @RequestBody RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            return AjaxResult.error(400, "Passwords do not match");
        }
        return AjaxResult.success("Register success");
    }

    public record LoginRequest(
            @NotBlank(message = "username must not be blank")
            String username,
            @NotBlank(message = "password must not be blank")
            String password,
            @NotBlank(message = "code must not be blank")
            String code,
            @NotBlank(message = "uuid must not be blank")
            String uuid
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "username must not be blank")
            String username,
            @NotBlank(message = "password must not be blank")
            String password,
            @NotBlank(message = "confirmPassword must not be blank")
            String confirmPassword,
            @NotBlank(message = "code must not be blank")
            String code,
            @NotBlank(message = "uuid must not be blank")
            String uuid
    ) {
    }
}
