package com.zhihuitong.modules.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderBatchLinkUpsertRequest {

    @NotNull(message = "orderItemId must not be null")
    @Min(value = 1, message = "orderItemId must be greater than 0")
    private Long orderItemId;

    @NotNull(message = "batchId must not be null")
    @Min(value = 1, message = "batchId must be greater than 0")
    private Long batchId;

    @DecimalMin(value = "0", inclusive = false, message = "allocatedWeight must be greater than 0")
    private BigDecimal allocatedWeight;

    @DecimalMin(value = "0", inclusive = false, message = "allocatedQuantity must be greater than 0")
    private BigDecimal allocatedQuantity;

    private String remark;
}
