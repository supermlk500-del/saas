package com.zhihuitong.modules.plan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class PlanRescheduleRequest {

    @NotBlank(message = "rescheduleReason must not be blank")
    private String rescheduleReason;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
}
