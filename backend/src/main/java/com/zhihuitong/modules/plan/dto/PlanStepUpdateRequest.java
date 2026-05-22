package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlanStepUpdateRequest {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    @DecimalMin(value = "0", inclusive = true, message = "planHours must be greater than or equal to 0")
    private BigDecimal planHours;

    @Min(value = 1, message = "sequenceNo must be greater than 0")
    private Integer sequenceNo;

    private String status;

    private String remark;
}
