package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GanttTaskVo {

    private Long planStepId;

    private String stepName;

    private String machineName;

    private LocalDateTime start;

    private LocalDateTime end;

    private String status;
}
