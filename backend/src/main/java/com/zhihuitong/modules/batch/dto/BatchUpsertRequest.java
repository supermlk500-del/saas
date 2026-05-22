package com.zhihuitong.modules.batch.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BatchUpsertRequest {

    @NotBlank(message = "batchNo must not be blank")
    private String batchNo;

    @NotBlank(message = "supplier must not be blank")
    private String supplier;

    @NotNull(message = "inDate must not be null")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inDate;

    @DecimalMin(value = "0", inclusive = true, message = "weight must be greater than or equal to 0")
    private BigDecimal weight;

    @DecimalMin(value = "0", inclusive = true, message = "width must be greater than or equal to 0")
    private BigDecimal width;

    private String composition;

    private String note;
}
