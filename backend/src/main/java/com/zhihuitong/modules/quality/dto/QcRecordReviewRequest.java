package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QcRecordReviewRequest {

    @NotBlank(message = "reviewResult must not be blank")
    private String reviewResult;

    private String reviewer;

    private String reviewRemark;
}
