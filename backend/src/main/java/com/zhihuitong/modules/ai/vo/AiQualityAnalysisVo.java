package com.zhihuitong.modules.ai.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiQualityAnalysisVo {

    private Long inspectionId;
    private String resultJudge;
    private String defectType;
    private String severity;
    private String riskLevel;
    private int recentSampleCount;
    private int recentFailureCount;
    private int recentRecheckCount;
    private double recentDefectRate;
    private List<String> possibleCauses = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();
    private String report;
    private String model;
    private boolean aiGenerated;
    private String fallbackReason;
    private String notice = "根因结论为辅助分析，需结合现场工艺参数和人工复核确认。";
}
