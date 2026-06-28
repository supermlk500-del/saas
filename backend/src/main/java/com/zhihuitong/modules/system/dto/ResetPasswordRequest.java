package com.zhihuitong.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotNull private Long userId;
    @NotBlank @Size(min = 8, max = 64) private String password;
}
