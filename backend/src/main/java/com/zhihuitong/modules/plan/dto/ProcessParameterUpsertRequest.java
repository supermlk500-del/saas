package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ProcessParameterUpsertRequest {

    @NotNull(message = "planStepId must not be null")
    @Min(value = 1, message = "planStepId must be greater than 0")
    private Long planStepId;

    @NotBlank(message = "paramName must not be blank")
    private String paramName;

    @NotBlank(message = "paramValue must not be blank")
    private String paramValue;

    private String unit;

    private String paramType;

    @NotNull(message = "recordTime must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    private String remark;
}
