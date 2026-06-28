package com.zhihuitong.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScheduleSuggestionApplyRequest {

    private Long recordId;

    @NotBlank(message = "strategy must not be blank")
    private String strategy;

    private String confirmationRemark;
}
