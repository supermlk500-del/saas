package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchDetailVo {

    private Long batchId;

    private String batchNo;

    private String supplier;

    private LocalDateTime inDate;

    private BigDecimal weight;

    private BigDecimal width;

    private String composition;

    private String note;

    private String status;

    private List<BatchPlanSummaryVo> planSummary;

    private List<BatchQcSummaryVo> qcSummary;

    private long exceptionCount;
}
