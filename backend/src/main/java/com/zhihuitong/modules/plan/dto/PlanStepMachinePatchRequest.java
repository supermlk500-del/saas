package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanStepMachinePatchRequest {

    @NotNull(message = "machineId must not be null")
    @Min(value = 1, message = "machineId must be greater than 0")
    private Long machineId;
}
