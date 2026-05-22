package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchPlanSummaryVo {

    private Long planId;

    private String status;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;
}
