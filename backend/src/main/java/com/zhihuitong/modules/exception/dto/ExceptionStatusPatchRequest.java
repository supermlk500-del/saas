package com.zhihuitong.modules.exception.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExceptionStatusPatchRequest {

    @NotBlank(message = "status must not be blank")
    private String status;

    private String remark;
}
