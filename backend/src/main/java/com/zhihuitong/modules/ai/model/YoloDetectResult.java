package com.zhihuitong.modules.ai.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class YoloDetectResult {

    private String inspectType;

    private String resultJudge;

    private BigDecimal confidenceScore;

    private String defectType;

    private String resultValue;

    private String imageUrl;

    private String sourceImageUrl;

    private List<YoloBox> boxes = new ArrayList<>();
}
