package com.zhihuitong.modules.plan.load;

import com.zhihuitong.modules.plan.vo.PlanMachineLoadItemVo;
import com.zhihuitong.modules.plan.vo.PlanMachineLoadSummaryVo;
import com.zhihuitong.modules.plan.vo.PlanStepVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanMachineLoadService {

    public PlanMachineLoadSummaryVo summarize(Long planId, List<PlanStepVo> steps) {
        Map<Long, MachineLoadAccumulator> assignedLoads = new LinkedHashMap<>();
        int unassignedStepCount = 0;

        for (PlanStepVo step : steps) {
            if (step.getMachineId() == null) {
                unassignedStepCount++;
                continue;
            }
            assignedLoads.computeIfAbsent(step.getMachineId(), key -> new MachineLoadAccumulator(
                    step.getMachineId(),
                    step.getMachineName()
            )).add(step);
        }

        List<PlanMachineLoadItemVo> loads = assignedLoads.values().stream()
                .map(MachineLoadAccumulator::toVo)
                .sorted(Comparator
                        .comparing(PlanMachineLoadItemVo::getTotalPlanHours, Comparator.reverseOrder())
                        .thenComparing(PlanMachineLoadItemVo::getMachineId))
                .toList();

        PlanMachineLoadSummaryVo summary = new PlanMachineLoadSummaryVo();
        summary.setPlanId(planId);
        summary.setTotalStepCount(steps.size());
        summary.setAssignedStepCount(steps.size() - unassignedStepCount);
        summary.setUnassignedStepCount(unassignedStepCount);
        summary.setMachineCount(loads.size());
        summary.setTotalPlanHours(loads.stream()
                .map(PlanMachineLoadItemVo::getTotalPlanHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP));
        summary.setMachines(loads);
        return summary;
    }

    private static class MachineLoadAccumulator {
        private final Long machineId;
        private final String machineName;
        private int stepCount;
        private BigDecimal totalPlanHours = BigDecimal.ZERO;
        private LocalDateTime firstStartTime;
        private LocalDateTime lastEndTime;

        private MachineLoadAccumulator(Long machineId, String machineName) {
            this.machineId = machineId;
            this.machineName = machineName;
        }

        private void add(PlanStepVo step) {
            stepCount++;
            if (step.getPlanHours() != null) {
                totalPlanHours = totalPlanHours.add(step.getPlanHours());
            }
            if (step.getPlanStartTime() != null && (firstStartTime == null || step.getPlanStartTime().isBefore(firstStartTime))) {
                firstStartTime = step.getPlanStartTime();
            }
            if (step.getPlanEndTime() != null && (lastEndTime == null || step.getPlanEndTime().isAfter(lastEndTime))) {
                lastEndTime = step.getPlanEndTime();
            }
        }

        private PlanMachineLoadItemVo toVo() {
            PlanMachineLoadItemVo item = new PlanMachineLoadItemVo();
            item.setMachineId(machineId);
            item.setMachineName(machineName);
            item.setStepCount(stepCount);
            item.setTotalPlanHours(totalPlanHours.setScale(2, RoundingMode.HALF_UP));
            item.setFirstStartTime(firstStartTime);
            item.setLastEndTime(lastEndTime);
            return item;
        }
    }
}
