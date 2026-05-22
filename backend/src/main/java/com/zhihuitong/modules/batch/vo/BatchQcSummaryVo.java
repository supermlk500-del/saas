package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchQcSummaryVo {

    private Long inspectionId;

    private Long planStepId;

    private String resultJudge;

    private String inspectType;

    private LocalDateTime inspectTime;
}
