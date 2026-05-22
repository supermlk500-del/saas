package com.zhihuitong.modules.process.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CapabilityUpsertRequest {

    @NotNull(message = "stepId must not be null")
    @Min(value = 1, message = "stepId must be greater than 0")
    private Long stepId;

    @NotNull(message = "machineId must not be null")
    @Min(value = 1, message = "machineId must be greater than 0")
    private Long machineId;

    @DecimalMin(value = "0", inclusive = true, message = "minWidth must be greater than or equal to 0")
    private BigDecimal minWidth;

    @DecimalMin(value = "0", inclusive = true, message = "maxWidth must be greater than or equal to 0")
    private BigDecimal maxWidth;

    @DecimalMin(value = "0", inclusive = true, message = "maxSpeed must be greater than or equal to 0")
    private BigDecimal maxSpeed;

    @DecimalMin(value = "0", inclusive = true, message = "maxBatchWeight must be greater than or equal to 0")
    private BigDecimal maxBatchWeight;

    @NotNull(message = "isActive must not be null")
    @Min(value = 0, message = "isActive must be 0 or 1")
    @Max(value = 1, message = "isActive must be 0 or 1")
    private Integer isActive;
}
