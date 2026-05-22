package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class InspectionDataUpsertRequest {

    @NotNull(message = "qcRecordId must not be null")
    @Min(value = 1, message = "qcRecordId must be greater than 0")
    private Long qcRecordId;

    private Long cameraId;

    @NotBlank(message = "fileType must not be blank")
    private String fileType;

    @NotBlank(message = "filePath must not be blank")
    private String filePath;

    @NotBlank(message = "fileName must not be blank")
    private String fileName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime captureTime;

    private String resultSummary;

    private String remark;
}
