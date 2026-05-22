package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusPatchRequest {

    @NotBlank(message = "status must not be blank")
    private String status;

    private String reason;
}
