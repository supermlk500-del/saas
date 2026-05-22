package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ProductionPlanCreateRequest {

    @NotNull(message = "batchId must not be null")
    @Min(value = 1, message = "batchId must be greater than 0")
    private Long batchId;

    @NotNull(message = "routeId must not be null")
    @Min(value = 1, message = "routeId must be greater than 0")
    private Long routeId;

    @NotNull(message = "planStartTime must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    private String remark;
}
