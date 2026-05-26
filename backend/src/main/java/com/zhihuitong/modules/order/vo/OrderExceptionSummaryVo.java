package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderExceptionSummaryVo {

    private Long exceptionId;

    private Long planStepId;

    private Long planId;

    private String stepName;

    private String batchNo;

    private String exceptionType;

    private String exceptionLevel;

    private String description;

    private String status;

    private LocalDateTime createTime;
}
