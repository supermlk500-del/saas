package com.zhihuitong.modules.process.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessRouteUpsertRequest {

    @NotBlank(message = "routeName must not be blank")
    private String routeName;

    private String description;

    @NotNull(message = "isActive must not be null")
    @Min(value = 0, message = "isActive must be 0 or 1")
    @Max(value = 1, message = "isActive must be 0 or 1")
    private Integer isActive;
}
