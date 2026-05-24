package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class QcDetectImageRequest {

    @NotNull(message = "planStepId must not be null")
    @Min(value = 1, message = "planStepId must be greater than 0")
    private Long planStepId;

    @NotNull(message = "qcItemId must not be null")
    @Min(value = 1, message = "qcItemId must be greater than 0")
    private Long qcItemId;

    private Long cameraId;

    private String inspector;

    private String remark;

    private MultipartFile file;
}
