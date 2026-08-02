package com.zhihuitong.modules.ai.dto;

import lombok.Data;

@Data
public class BrowserInferenceDecoder {

    private String type = "yolo-raw";

    private String outputLayout = "AUTO";

    private String boxFormat = "cxcywh";

    private boolean hasObjectness = false;

    private boolean coordinatesNormalized = false;
}
