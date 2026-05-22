package com.zhihuitong.modules.exception.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExceptionCloseRequest {

    @NotBlank(message = "handleResult must not be blank")
    private String handleResult;

    private String closeRemark;
}
