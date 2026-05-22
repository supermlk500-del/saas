package com.zhihuitong.modules.process.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MachineStatusPatchRequest {

    @NotBlank(message = "status must not be blank")
    private String status;

    private String reason;
}
