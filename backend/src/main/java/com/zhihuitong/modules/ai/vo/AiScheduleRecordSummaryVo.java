package com.zhihuitong.modules.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiScheduleRecordSummaryVo {

    private Long recordId;
    private Long planId;
    private String orderNo;
    private String batchNo;
    private String status;
    private String selectedStrategy;
    private LocalDateTime createTime;
    private LocalDateTime appliedTime;
}
