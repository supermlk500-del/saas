package com.zhihuitong.modules.order.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderSchedulePoolQuery extends PageQuery {

    private String orderNo;

    private String customerName;

    private String status;

    private String priority;

    private Boolean readyForSchedule;
}
