package com.zhihuitong.modules.plan.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.plan.vo.PlanReadinessIssueVo;
import com.zhihuitong.modules.plan.vo.PlanReadinessVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlanReadinessService {

    private static final String BLOCKER = "BLOCKER";
    private static final String WARNING = "WARNING";

    private final ProductionPlanMapper productionPlanMapper;
    private final PlanStepMapper planStepMapper;
    private final OrderService orderService;

    public PlanReadinessService(ProductionPlanMapper productionPlanMapper,
                                PlanStepMapper planStepMapper,
                                OrderService orderService) {
        this.productionPlanMapper = productionPlanMapper;
        this.planStepMapper = planStepMapper;
        this.orderService = orderService;
    }

    public PlanReadinessVo check(Long planId) {
        ProductionPlan plan = productionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "生产计划不存在");
        }
        List<PlanStep> steps = planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                .eq(PlanStep::getPlanId, planId)
                .orderByAsc(PlanStep::getSequenceNo));

        List<PlanReadinessIssueVo> issues = new ArrayList<>();
        if (steps.isEmpty()) {
            issues.add(issue(BLOCKER, "PLAN_NO_STEPS", null, "生产计划没有工序", "先生成或补充计划工序"));
        }
        for (PlanStep step : steps) {
            collectStepIssues(step, issues);
        }
        collectDeliveryIssue(plan, issues);

        int blockerCount = (int) issues.stream().filter(item -> BLOCKER.equals(item.getSeverity())).count();
        int warningCount = (int) issues.stream().filter(item -> WARNING.equals(item.getSeverity())).count();

        PlanReadinessVo result = new PlanReadinessVo();
        result.setPlanId(planId);
        result.setReady(blockerCount == 0);
        result.setTotalStepCount(steps.size());
        result.setBlockerCount(blockerCount);
        result.setWarningCount(warningCount);
        result.setIssues(issues);
        return result;
    }

    private void collectStepIssues(PlanStep step, List<PlanReadinessIssueVo> issues) {
        if (step.getMachineId() == null) {
            issues.add(issue(
                    BLOCKER,
                    "MACHINE_UNASSIGNED",
                    step.getPlanStepId(),
                    "工序未分配机台",
                    "先为该工序分配支持当前批次的可用机台"
            ));
        }
        if (step.getPlanStartTime() == null || step.getPlanEndTime() == null) {
            issues.add(issue(
                    BLOCKER,
                    "TIME_WINDOW_MISSING",
                    step.getPlanStepId(),
                    "工序缺少计划开始或结束时间",
                    "补充工序时间，或重新生成生产计划"
            ));
        }
        if ("ABNORMAL".equals(step.getStatus())) {
            issues.add(issue(
                    BLOCKER,
                    "STEP_ABNORMAL",
                    step.getPlanStepId(),
                    "工序状态异常",
                    "处理异常记录后再发布或执行计划"
            ));
        }
    }

    private void collectDeliveryIssue(ProductionPlan plan, List<PlanReadinessIssueVo> issues) {
        if (plan.getOrderId() == null || plan.getPlanEndTime() == null) {
            return;
        }
        OrderInfo order = orderService.requireOrder(plan.getOrderId());
        LocalDateTime deliveryDate = order.getDeliveryDate();
        if (deliveryDate != null && plan.getPlanEndTime().isAfter(deliveryDate)) {
            issues.add(issue(
                    WARNING,
                    "DELIVERY_DATE_RISK",
                    null,
                    "计划结束时间晚于订单交期",
                    "评估是否提前计划开始时间、调整机台或拆分批次"
            ));
        }
    }

    private PlanReadinessIssueVo issue(String severity,
                                       String code,
                                       Long planStepId,
                                       String message,
                                       String suggestion) {
        PlanReadinessIssueVo item = new PlanReadinessIssueVo();
        item.setSeverity(severity);
        item.setCode(code);
        item.setPlanStepId(planStepId);
        item.setMessage(message);
        item.setSuggestion(suggestion);
        return item;
    }
}
