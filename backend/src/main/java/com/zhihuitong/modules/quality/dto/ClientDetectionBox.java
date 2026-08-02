package com.zhihuitong.modules.quality.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientDetectionBox {

    @NotNull(message = "classIndex must not be null")
    @Min(value = 0, message = "classIndex must be greater than or equal to 0")
    private Integer classIndex;

    @NotBlank(message = "code must not be blank")
    private String code;

    private String label;

    @NotNull(message = "score must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "score must be between 0 and 1")
    @DecimalMax(value = "1.0", inclusive = true, message = "score must be between 0 and 1")
    private Double score;

    @NotNull(message = "x1 must not be null")
    private Double x1;

    @NotNull(message = "y1 must not be null")
    private Double y1;

    @NotNull(message = "x2 must not be null")
    private Double x2;

    @NotNull(message = "y2 must not be null")
    private Double y2;
}
