package com.zhihuitong.modules.exception.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ExceptionReworkRequest {

    @NotBlank(message = "reworkPlan must not be blank")
    private String reworkPlan;

    private String reworkOwner;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedFinishTime;
}
