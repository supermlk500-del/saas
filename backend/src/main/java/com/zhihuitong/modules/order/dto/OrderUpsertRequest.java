package com.zhihuitong.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class OrderUpsertRequest {

    @NotBlank(message = "orderNo must not be blank")
    private String orderNo;

    @NotBlank(message = "customerName must not be blank")
    private String customerName;

    @NotNull(message = "orderDate must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;

    @NotNull(message = "deliveryDate must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveryDate;

    private String priority;

    @NotBlank(message = "status must not be blank")
    private String status;

    private String remark;
}
