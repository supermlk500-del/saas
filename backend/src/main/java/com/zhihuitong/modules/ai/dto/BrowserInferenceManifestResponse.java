package com.zhihuitong.modules.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class BrowserInferenceManifestResponse {

    private String modelName;

    private String modelVersion;

    private String modelUrl;

    private String sha256;

    private long fileSize;

    private String inputName;

    private String outputName;

    private int inputWidth;

    private int inputHeight;

    private String inputLayout = "NCHW";

    private String inputColor = "RGB";

    private float normalizationScale = 255.0F;

    private int letterboxFill = 114;

    private BrowserInferenceDecoder decoder;

    private List<BrowserInferenceClassItem> classes;

    private float confidenceThreshold;

    private float iouThreshold;

    private float realtimeConfidenceThreshold;

    private int targetInferenceFps = 60;

    private int continuousHitFrames = 3;

    private int evidenceImageCount = 1;

    private List<String> preferredExecutionProviders = List.of("webgpu", "wasm");
}
