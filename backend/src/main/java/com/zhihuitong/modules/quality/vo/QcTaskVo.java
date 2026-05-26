package com.zhihuitong.modules.quality.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QcTaskVo {

    private String taskNo;

    private Long planStepId;

    private Long planId;

    private Long stepId;

    private String stepName;

    private Long machineId;

    private String machineName;

    private Long orderId;

    private String orderNo;

    private Long orderItemId;

    private Long batchId;

    private String batchNo;

    private LocalDateTime latestInspectTime;

    private String latestJudge;

    private String status;
}
