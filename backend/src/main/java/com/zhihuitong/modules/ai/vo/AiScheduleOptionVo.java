package com.zhihuitong.modules.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiScheduleOptionVo {

    private String strategy;
    private String strategyName;
    private String solverStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalMinutes;
    private long delayMinutes;
    private int machineChanges;
    private int estimatedUtilizationScore;
    private String explanation;
    private String model;
    private boolean aiGenerated;
    private String fallbackReason;
    private List<AiScheduleStepVo> steps = new ArrayList<>();
}
