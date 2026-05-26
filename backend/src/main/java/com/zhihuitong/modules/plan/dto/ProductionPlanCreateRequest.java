package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ProductionPlanCreateRequest {

    @NotNull(message = "orderId 不能为空")
    @Min(value = 1, message = "orderId 必须大于 0")
    private Long orderId;

    @NotNull(message = "orderItemId 不能为空")
    @Min(value = 1, message = "orderItemId 必须大于 0")
    private Long orderItemId;

    @NotNull(message = "batchId 不能为空")
    @Min(value = 1, message = "batchId 必须大于 0")
    private Long batchId;

    @NotNull(message = "routeId 不能为空")
    @Min(value = 1, message = "routeId 必须大于 0")
    private Long routeId;

    @NotNull(message = "planStartTime 不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    private String remark;
}
