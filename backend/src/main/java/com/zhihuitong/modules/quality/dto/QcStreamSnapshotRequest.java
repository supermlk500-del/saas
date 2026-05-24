package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QcStreamSnapshotRequest {

    private MultipartFile file;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime frameTime;

    private String resultJudge;

    @DecimalMin(value = "0", inclusive = true, message = "confidenceScore must be greater than or equal to 0")
    private BigDecimal confidenceScore;

    private String resultValue;

    private String remark;
}
