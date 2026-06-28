package com.zhihuitong.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "username must not be blank")
    @Size(max = 64, message = "username must not exceed 64 characters")
    private String username;
    @NotBlank(message = "password must not be blank")
    @Size(max = 128, message = "password must not exceed 128 characters")
    private String password;
    @Size(max = 10, message = "captchaCode must not exceed 10 characters")
    private String captchaCode;
    @Size(max = 64, message = "captchaUuid must not exceed 64 characters")
    private String captchaUuid;
}