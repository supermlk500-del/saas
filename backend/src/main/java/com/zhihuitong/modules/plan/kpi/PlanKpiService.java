package com.zhihuitong.modules.plan.kpi;

import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.vo.PlanKpiVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class PlanKpiService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public PlanKpiVo calculate(ProductionPlan plan, List<PlanStep> steps, OrderInfo orderInfo) {
        int totalStepCount = steps.size();
        int assignedStepCount = (int) steps.stream().filter(step -> step.getMachineId() != null).count();
        int unassignedStepCount = totalStepCount - assignedStepCount;
        int algorithmRemarkCount = (int) steps.stream().filter(this::hasAlgorithmRemark).count();

        LocalDateTime deliveryDate = orderInfo == null ? null : orderInfo.getDeliveryDate();
        int onTimeStepCount = 0;
        int lateStepCount = 0;
        long maxLateMinutes = 0;
        if (deliveryDate != null) {
            for (PlanStep step : steps) {
                LocalDateTime endTime = step.getPlanEndTime();
                if (endTime == null) {
                    continue;
                }
                if (endTime.isAfter(deliveryDate)) {
                    lateStepCount++;
                    maxLateMinutes = Math.max(maxLateMinutes, Duration.between(deliveryDate, endTime).toMinutes());
                } else {
                    onTimeStepCount++;
                }
            }
        }

        LocalDateTime plannedEndTime = resolvePlannedEndTime(plan, steps);
        Boolean deliveryDateMet = deliveryDate == null || plannedEndTime == null ? null : !plannedEndTime.isAfter(deliveryDate);

        PlanKpiVo vo = new PlanKpiVo();
        vo.setPlanId(plan.getPlanId());
        vo.setTotalStepCount(totalStepCount);
        vo.setAssignedStepCount(assignedStepCount);
        vo.setUnassignedStepCount(unassignedStepCount);
        vo.setMachineAssignmentRate(percent(assignedStepCount, totalStepCount));
        vo.setTotalPlanHours(totalPlanHours(steps));
        vo.setOnTimeStepCount(onTimeStepCount);
        vo.setLateStepCount(lateStepCount);
        vo.setOnTimeRate(percent(onTimeStepCount, onTimeStepCount + lateStepCount));
        vo.setMaxLateHours(roundHours(maxLateMinutes));
        vo.setPlannedEndTime(plannedEndTime);
        vo.setDeliveryDate(deliveryDate);
        vo.setDeliveryDateMet(deliveryDateMet);
        vo.setAlgorithmRemarkCount(algorithmRemarkCount);
        vo.setAlgorithmRemarkCoverageRate(percent(algorithmRemarkCount, totalStepCount));
        return vo;
    }

    private BigDecimal totalPlanHours(List<PlanStep> steps) {
        BigDecimal total = steps.stream()
                .map(PlanStep::getPlanHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime resolvePlannedEndTime(ProductionPlan plan, List<PlanStep> steps) {
        if (plan.getPlanEndTime() != null) {
            return plan.getPlanEndTime();
        }
        return steps.stream()
                .map(PlanStep::getPlanEndTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private boolean hasAlgorithmRemark(PlanStep step) {
        if (step.getRemark() == null) {
            return false;
        }
        String remark = step.getRemark().toLowerCase(Locale.ROOT);
        return remark.contains("算法") || remark.contains("algorithm") || remark.contains("greedy");
    }

    private BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal roundHours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
