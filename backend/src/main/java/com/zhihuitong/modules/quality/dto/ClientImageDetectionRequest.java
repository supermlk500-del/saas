package com.zhihuitong.modules.quality.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClientImageDetectionRequest {

    @NotNull(message = "planStepId must not be null")
    @Min(value = 1, message = "planStepId must be greater than 0")
    private Long planStepId;

    @NotNull(message = "qcItemId must not be null")
    @Min(value = 1, message = "qcItemId must be greater than 0")
    private Long qcItemId;

    private Long cameraId;

    private String inspector;

    private String remark;

    @NotBlank(message = "modelSha256 must not be blank")
    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "modelSha256 must be a 64 character hex string")
    private String modelSha256;

    @Min(value = 1, message = "imageWidth must be greater than 0")
    private int imageWidth;

    @Min(value = 1, message = "imageHeight must be greater than 0")
    private int imageHeight;

    @Min(value = 0, message = "preprocessTimeMs must be non-negative")
    private long preprocessTimeMs;

    @Min(value = 0, message = "inferenceTimeMs must be non-negative")
    private long inferenceTimeMs;

    @Min(value = 0, message = "postprocessTimeMs must be non-negative")
    private long postprocessTimeMs;

    @NotBlank(message = "providerStrategy must not be blank")
    @Pattern(regexp = "^(WebGPU|WASM)$", message = "providerStrategy must be WebGPU or WASM")
    private String providerStrategy;

    @Valid
    @Size(max = 100, message = "detections must not exceed 100")
    private List<ClientDetectionBox> detections = new ArrayList<>();
}
