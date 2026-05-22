package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BatchListVo {

    private Long batchId;

    private String batchNo;

    private String supplier;

    private LocalDateTime inDate;

    private BigDecimal weight;

    private BigDecimal width;

    private String composition;

    private String note;

    private String status;
}
