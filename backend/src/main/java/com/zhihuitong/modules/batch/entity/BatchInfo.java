package com.zhihuitong.modules.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("batchinfo")
public class BatchInfo {

    @TableId(value = "batchId", type = IdType.ASSIGN_ID)
    private Long batchId;

    private String batchNo;

    private String supplier;

    private LocalDateTime inDate;

    private BigDecimal weight;

    private BigDecimal width;

    private String composition;

    private String note;
}
