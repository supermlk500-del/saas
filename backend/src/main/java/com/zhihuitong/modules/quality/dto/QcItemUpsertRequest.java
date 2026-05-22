package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QcItemUpsertRequest {

    @NotBlank(message = "qcItemCode must not be blank")
    private String qcItemCode;

    @NotBlank(message = "qcItemName must not be blank")
    private String qcItemName;

    private String qcType;

    private String unit;

    @DecimalMin(value = "0", inclusive = true, message = "standardMin must be greater than or equal to 0")
    private BigDecimal standardMin;

    @DecimalMin(value = "0", inclusive = true, message = "standardMax must be greater than or equal to 0")
    private BigDecimal standardMax;

    @NotNull(message = "isActive must not be null")
    @Min(value = 0, message = "isActive must be 0 or 1")
    @Max(value = 1, message = "isActive must be 0 or 1")
    private Integer isActive;

    private String description;
}
