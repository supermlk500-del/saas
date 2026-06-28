package com.zhihuitong.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleStatusRequest {
    @NotNull private Long roleId;
    @NotBlank private String status;
}
