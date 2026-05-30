package com.zhihuitong.modules.plan.kpi;

import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.vo.PlanKpiVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanKpiServiceTest {

    private final PlanKpiService service = new PlanKpiService();

    @Test
    void calculateShouldSummarizeAssignmentDeliveryAndAlgorithmCoverage() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanId(101L);
        plan.setPlanEndTime(LocalDateTime.of(2026, 5, 30, 18, 0));

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setDeliveryDate(LocalDateTime.of(2026, 5, 30, 16, 0));

        PlanStep assignedOnTime = step(1L, 11L, "PENDING",
                LocalDateTime.of(2026, 5, 30, 10, 0),
                new BigDecimal("2.50"),
                "算法选择: greedy_eft");
        PlanStep assignedLate = step(2L, 12L, "PENDING",
                LocalDateTime.of(2026, 5, 30, 18, 0),
                new BigDecimal("1.25"),
                null);
        PlanStep unassigned = step(3L, null, "PENDING",
                null,
                null,
                "未匹配到可用机台");

        PlanKpiVo result = service.calculate(plan, List.of(assignedOnTime, assignedLate, unassigned), orderInfo);

        assertThat(result.getPlanId()).isEqualTo(101L);
        assertThat(result.getTotalStepCount()).isEqualTo(3);
        assertThat(result.getAssignedStepCount()).isEqualTo(2);
        assertThat(result.getUnassignedStepCount()).isEqualTo(1);
        assertThat(result.getMachineAssignmentRate()).isEqualByComparingTo("66.67");
        assertThat(result.getTotalPlanHours()).isEqualByComparingTo("3.75");
        assertThat(result.getOnTimeStepCount()).isEqualTo(1);
        assertThat(result.getLateStepCount()).isEqualTo(1);
        assertThat(result.getOnTimeRate()).isEqualByComparingTo("50.00");
        assertThat(result.getMaxLateHours()).isEqualByComparingTo("2.00");
        assertThat(result.getDeliveryDateMet()).isFalse();
        assertThat(result.getAlgorithmRemarkCount()).isEqualTo(1);
        assertThat(result.getAlgorithmRemarkCoverageRate()).isEqualByComparingTo("33.33");
    }

    private PlanStep step(Long stepId,
                          Long machineId,
                          String status,
                          LocalDateTime endTime,
                          BigDecimal planHours,
                          String remark) {
        PlanStep step = new PlanStep();
        step.setStepId(stepId);
        step.setMachineId(machineId);
        step.setStatus(status);
        step.setPlanEndTime(endTime);
        step.setPlanHours(planHours);
        step.setRemark(remark);
        return step;
    }
}
