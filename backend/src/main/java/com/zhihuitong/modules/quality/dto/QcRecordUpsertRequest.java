package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QcRecordUpsertRequest {

    @NotNull(message = "planStepId must not be null")
    @Min(value = 1, message = "planStepId must be greater than 0")
    private Long planStepId;

    @NotNull(message = "qcItemId must not be null")
    @Min(value = 1, message = "qcItemId must be greater than 0")
    private Long qcItemId;

    @NotNull(message = "inspectTime must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inspectTime;

    @NotBlank(message = "inspectType must not be blank")
    private String inspectType;

    private Long cameraId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime frameTime;

    private String imageUrl;

    @DecimalMin(value = "0", inclusive = true, message = "confidenceScore must be greater than or equal to 0")
    private BigDecimal confidenceScore;

    private String resultValue;

    @NotBlank(message = "resultJudge must not be blank")
    private String resultJudge;

    private String inspector;

    private String remark;
}
