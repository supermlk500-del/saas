package com.zhihuitong.modules.plan.load;

import com.zhihuitong.modules.plan.vo.PlanMachineLoadSummaryVo;
import com.zhihuitong.modules.plan.vo.PlanStepVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanMachineLoadServiceTest {

    private final PlanMachineLoadService service = new PlanMachineLoadService();

    @Test
    void summarizeShouldAggregateStepsByMachine() {
        PlanStepVo first = step(1L, 501L, "染缸-01",
                LocalDateTime.of(2026, 5, 30, 8, 0),
                LocalDateTime.of(2026, 5, 30, 10, 0),
                "2.00");
        PlanStepVo second = step(2L, 501L, "染缸-01",
                LocalDateTime.of(2026, 5, 30, 11, 0),
                LocalDateTime.of(2026, 5, 30, 14, 30),
                "3.50");
        PlanStepVo third = step(3L, 502L, "定型机-01",
                LocalDateTime.of(2026, 5, 30, 9, 0),
                LocalDateTime.of(2026, 5, 30, 10, 30),
                "1.50");
        PlanStepVo unassigned = step(4L, null, null, null, null, null);

        PlanMachineLoadSummaryVo result = service.summarize(101L, List.of(first, second, third, unassigned));

        assertThat(result.getPlanId()).isEqualTo(101L);
        assertThat(result.getTotalStepCount()).isEqualTo(4);
        assertThat(result.getAssignedStepCount()).isEqualTo(3);
        assertThat(result.getUnassignedStepCount()).isEqualTo(1);
        assertThat(result.getMachineCount()).isEqualTo(2);
        assertThat(result.getTotalPlanHours()).isEqualByComparingTo("7.00");
        assertThat(result.getMachines()).hasSize(2);
        assertThat(result.getMachines().get(0).getMachineId()).isEqualTo(501L);
        assertThat(result.getMachines().get(0).getStepCount()).isEqualTo(2);
        assertThat(result.getMachines().get(0).getTotalPlanHours()).isEqualByComparingTo("5.50");
        assertThat(result.getMachines().get(0).getFirstStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 30, 8, 0));
        assertThat(result.getMachines().get(0).getLastEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 30, 14, 30));
    }

    private PlanStepVo step(Long planStepId,
                            Long machineId,
                            String machineName,
                            LocalDateTime startTime,
                            LocalDateTime endTime,
                            String planHours) {
        PlanStepVo step = new PlanStepVo();
        step.setPlanStepId(planStepId);
        step.setPlanId(101L);
        step.setMachineId(machineId);
        step.setMachineName(machineName);
        step.setPlanStartTime(startTime);
        step.setPlanEndTime(endTime);
        step.setPlanHours(planHours == null ? null : new BigDecimal(planHours));
        step.setStatus("PENDING");
        return step;
    }
}
