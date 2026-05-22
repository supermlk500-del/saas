package com.zhihuitong.modules.exception.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ExceptionRecordUpsertRequest {

    @NotNull(message = "planStepId must not be null")
    @Min(value = 1, message = "planStepId must be greater than 0")
    private Long planStepId;

    @NotBlank(message = "exceptionType must not be blank")
    private String exceptionType;

    @NotBlank(message = "exceptionLevel must not be blank")
    private String exceptionLevel;

    @NotBlank(message = "description must not be blank")
    private String description;

    private String handleResult;

    @NotNull(message = "createTime must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @NotBlank(message = "status must not be blank")
    private String status;
}
