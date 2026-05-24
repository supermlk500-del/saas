package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
public class QcDetectFrameRequest {

    @NotNull(message = "planStepId must not be null")
    @Min(value = 1, message = "planStepId must be greater than 0")
    private Long planStepId;

    @NotNull(message = "qcItemId must not be null")
    @Min(value = 1, message = "qcItemId must be greater than 0")
    private Long qcItemId;

    @NotNull(message = "cameraId must not be null")
    @Min(value = 1, message = "cameraId must be greater than 0")
    private Long cameraId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime frameTime;

    private String inspector;

    private String remark;

    private MultipartFile file;
}
