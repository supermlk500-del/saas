package com.zhihuitong.modules.quality.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QcTaskQuery extends PageQuery {

    private Long planId;

    private Long machineId;

    private Long orderId;

    private Long orderItemId;

    private Long batchId;

    private String keyword;

    private String status;
}
