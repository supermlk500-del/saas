package com.zhihuitong.modules.plan.algorithm;

import com.zhihuitong.common.enums.DeviceStatus;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.service.MachineService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SchedulingAlgorithmService {

    private static final BigDecimal MIN_PLAN_HOURS = BigDecimal.valueOf(0.25);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final MachineService machineService;

    public SchedulingAlgorithmService(MachineService machineService) {
        this.machineService = machineService;
    }

    public ScheduleMachineAssignment chooseEarliestFinishMachine(ProcessStep step,
                                                                 BatchInfo batch,
                                                                 LocalDateTime earliestStart,
                                                                 List<StepMachineCapability> capabilities) {
        BigDecimal fallbackHours = resolveFallbackHours(step);
        if (capabilities == null || capabilities.isEmpty()) {
            return ScheduleMachineAssignment.unassigned(earliestStart, fallbackHours, "算法未找到满足幅宽/批重的启用机台能力");
        }

        return capabilities.stream()
                .map(capability -> buildCandidate(step, batch, earliestStart, capability))
                .filter(Candidate::available)
                .min(Comparator
                        .comparing(Candidate::endTime)
                        .thenComparing(Candidate::statusRank)
                        .thenComparing(Candidate::speedRank)
                        .thenComparing(Candidate::machineId))
                .map(candidate -> ScheduleMachineAssignment.assigned(
                        candidate.machineId(),
                        earliestStart,
                        candidate.endTime(),
                        candidate.planHours(),
                        candidate.reason()
                ))
                .orElseGet(() -> ScheduleMachineAssignment.unassigned(earliestStart, fallbackHours, "算法过滤后无可用机台"));
    }

    private Candidate buildCandidate(ProcessStep step,
                                     BatchInfo batch,
                                     LocalDateTime earliestStart,
                                     StepMachineCapability capability) {
        Machine machine = machineService.requireMachine(capability.getMachineId());
        BigDecimal planHours = estimatePlanHours(step, batch, capability);
        LocalDateTime endTime = earliestStart.plusMinutes(planHours.multiply(MINUTES_PER_HOUR).longValue());
        boolean available = isAvailable(machine);
        String reason = buildReason(machine, capability, planHours, available);
        return new Candidate(
                capability.getMachineId(),
                endTime,
                planHours,
                machineStatusRank(machine),
                speedRank(capability),
                available,
                reason
        );
    }

    public BigDecimal estimatePlanHours(ProcessStep step, BatchInfo batch, StepMachineCapability capability) {
        BigDecimal fallbackHours = resolveFallbackHours(step);
        BigDecimal batchWeight = batch == null ? null : batch.getWeight();
        BigDecimal maxSpeed = capability == null ? null : capability.getMaxSpeed();
        if (batchWeight == null || maxSpeed == null || maxSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            return fallbackHours;
        }
        BigDecimal speedHours = batchWeight.divide(maxSpeed, 2, RoundingMode.CEILING);
        if (speedHours.compareTo(MIN_PLAN_HOURS) < 0) {
            return MIN_PLAN_HOURS;
        }
        return speedHours;
    }

    private BigDecimal resolveFallbackHours(ProcessStep step) {
        if (step == null || step.getDefaultHours() == null || step.getDefaultHours().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return step.getDefaultHours();
    }

    private boolean isAvailable(Machine machine) {
        Integer status = machine.getStatus();
        return status == null
                || status == DeviceStatus.IDLE.getCode()
                || status == DeviceStatus.RUNNING.getCode();
    }

    private int machineStatusRank(Machine machine) {
        Integer status = machine.getStatus();
        if (status == null) {
            return 10;
        }
        if (status == DeviceStatus.IDLE.getCode()) {
            return 0;
        }
        if (status == DeviceStatus.RUNNING.getCode()) {
            return 1;
        }
        return 99;
    }

    private BigDecimal speedRank(StepMachineCapability capability) {
        BigDecimal maxSpeed = capability.getMaxSpeed();
        if (maxSpeed == null || maxSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return maxSpeed.negate();
    }

    private String buildReason(Machine machine,
                               StepMachineCapability capability,
                               BigDecimal planHours,
                               boolean available) {
        if (!available) {
            return "算法排除不可用机台: " + machine.getMachineName();
        }
        String speed = capability.getMaxSpeed() == null ? "默认工时" : capability.getMaxSpeed().stripTrailingZeros().toPlainString();
        String machineName = StringUtils.hasText(machine.getMachineName()) ? machine.getMachineName() : machine.getMachineCode();
        return "算法选择: greedy_eft, 机台=" + machineName + ", 速度=" + speed + ", 预计工时=" + planHours.stripTrailingZeros().toPlainString() + "h";
    }

    private record Candidate(Long machineId,
                             LocalDateTime endTime,
                             BigDecimal planHours,
                             int statusRank,
                             BigDecimal speedRank,
                             boolean available,
                             String reason) {
    }
}
