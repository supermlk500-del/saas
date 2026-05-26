package com.zhihuitong.modules.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemUpsertRequest {

    @NotBlank(message = "productCode must not be blank")
    private String productCode;

    @NotBlank(message = "productName must not be blank")
    private String productName;

    private String specification;

    private String color;

    @NotNull(message = "quantity must not be null")
    @DecimalMin(value = "0", inclusive = false, message = "quantity must be greater than 0")
    private BigDecimal quantity;

    private String unit;

    @DecimalMin(value = "0", inclusive = true, message = "requiredWidth must be greater than or equal to 0")
    private BigDecimal requiredWidth;

    @DecimalMin(value = "0", inclusive = true, message = "requiredWeight must be greater than or equal to 0")
    private BigDecimal requiredWeight;

    private String remark;
}
