package com.zhihuitong.modules.process.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessStepUpsertRequest {

    @NotBlank(message = "stepCode must not be blank")
    private String stepCode;

    @NotBlank(message = "stepName must not be blank")
    private String stepName;

    private String stepType;

    @Min(value = 0, message = "sortOrder must be greater than or equal to 0")
    private Integer sortOrder;

    @DecimalMin(value = "0", inclusive = true, message = "defaultHours must be greater than or equal to 0")
    private BigDecimal defaultHours;

    private String description;

    @NotNull(message = "isActive must not be null")
    @Min(value = 0, message = "isActive must be 0 or 1")
    @Max(value = 1, message = "isActive must be 0 or 1")
    private Integer isActive;
}
