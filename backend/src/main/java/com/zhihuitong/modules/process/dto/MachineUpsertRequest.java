package com.zhihuitong.modules.process.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MachineUpsertRequest {

    @NotBlank(message = "machineCode must not be blank")
    private String machineCode;

    @NotBlank(message = "machineName must not be blank")
    private String machineName;

    private String machineType;

    private String description;

    @NotBlank(message = "status must not be blank")
    private String status;
}
