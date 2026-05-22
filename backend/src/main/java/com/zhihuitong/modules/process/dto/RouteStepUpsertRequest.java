package com.zhihuitong.modules.process.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RouteStepUpsertRequest {

    @NotNull(message = "stepId must not be null")
    @Min(value = 1, message = "stepId must be greater than 0")
    private Long stepId;

    @NotNull(message = "sortOrder must not be null")
    @Min(value = 1, message = "sortOrder must be greater than 0")
    private Integer sortOrder;

    @NotNull(message = "isMandatory must not be null")
    @Min(value = 0, message = "isMandatory must be 0 or 1")
    @Max(value = 1, message = "isMandatory must be 0 or 1")
    private Integer isMandatory;
}
