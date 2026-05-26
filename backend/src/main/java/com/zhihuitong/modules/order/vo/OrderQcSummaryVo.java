package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderQcSummaryVo {

    private Long inspectionId;

    private Long planStepId;

    private Long qcItemId;

    private Long planId;

    private String stepName;

    private String batchNo;

    private String resultJudge;

    private String resultValue;

    private String inspectType;

    private LocalDateTime inspectTime;
}
