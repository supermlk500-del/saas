package com.zhihuitong.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusPatchRequest {

    @NotBlank(message = "status must not be blank")
    private String status;

    private String reason;
}
