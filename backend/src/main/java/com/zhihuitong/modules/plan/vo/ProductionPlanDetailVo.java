package com.zhihuitong.modules.plan.vo;

import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.process.entity.ProcessRoute;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductionPlanDetailVo {

    private Long planId;

    private OrderInfo orderInfo;

    private OrderItem orderItemInfo;

    private BatchInfo batchInfo;

    private ProcessRoute routeInfo;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private String status;

    private String remark;

    private List<PlanStepVo> planSteps;
}
