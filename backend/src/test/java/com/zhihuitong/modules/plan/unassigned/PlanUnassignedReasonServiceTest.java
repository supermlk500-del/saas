package com.zhihuitong.modules.plan.unassigned;

import com.zhihuitong.modules.plan.vo.PlanStepVo;
import com.zhihuitong.modules.plan.vo.PlanUnassignedReasonVo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanUnassignedReasonServiceTest {

    private final PlanUnassignedReasonService service = new PlanUnassignedReasonService();

    @Test
    void analyzeShouldReturnReasonsForUnassignedAbnormalAndMissingTimeSteps() {
        PlanStepVo unassigned = step(1L, null, "染色", "PENDING", null);
        unassigned.setRemark("计划生成时未匹配到可用的启用机台能力");

        PlanStepVo abnormal = step(2L, 20L, "定型", "ABNORMAL", LocalDateTime.of(2026, 5, 30, 12, 0));
        abnormal.setRemark("设备停机");

        PlanStepVo missingTime = step(3L, 30L, "后整理", "PENDING", null);

        PlanStepVo normal = step(4L, 40L, "检验", "PENDING", LocalDateTime.of(2026, 5, 30, 16, 0));

        PlanUnassignedReasonVo result = service.analyze(101L, List.of(unassigned, abnormal, missingTime, normal));

        assertThat(result.getPlanId()).isEqualTo(101L);
        assertThat(result.getTotalIssueCount()).isEqualTo(3);
        assertThat(result.getUnassignedCount()).isEqualTo(1);
        assertThat(result.getAbnormalCount()).isEqualTo(1);
        assertThat(result.getMissingTimeCount()).isEqualTo(1);
        assertThat(result.getItems())
                .extracting("reasonCode")
                .containsExactly("MACHINE_UNASSIGNED", "STEP_ABNORMAL", "TIME_WINDOW_MISSING");
    }

    private PlanStepVo step(Long planStepId,
                            Long machineId,
                            String stepName,
                            String status,
                            LocalDateTime endTime) {
        PlanStepVo step = new PlanStepVo();
        step.setPlanStepId(planStepId);
        step.setStepId(planStepId + 100);
        step.setStepName(stepName);
        step.setSequenceNo(planStepId.intValue());
        step.setMachineId(machineId);
        step.setStatus(status);
        step.setPlanStartTime(endTime == null ? null : endTime.minusHours(1));
        step.setPlanEndTime(endTime);
        return step;
    }
}
