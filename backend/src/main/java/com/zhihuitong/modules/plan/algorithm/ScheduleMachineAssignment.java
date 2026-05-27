package com.zhihuitong.modules.plan.algorithm;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ScheduleMachineAssignment {

    private final Long machineId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final BigDecimal planHours;
    private final String reason;

    private ScheduleMachineAssignment(Long machineId,
                                      LocalDateTime startTime,
                                      LocalDateTime endTime,
                                      BigDecimal planHours,
                                      String reason) {
        this.machineId = machineId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.planHours = planHours;
        this.reason = reason;
    }

    public static ScheduleMachineAssignment assigned(Long machineId,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     BigDecimal planHours,
                                                     String reason) {
        return new ScheduleMachineAssignment(machineId, startTime, endTime, planHours, reason);
    }

    public static ScheduleMachineAssignment unassigned(LocalDateTime startTime,
                                                       BigDecimal planHours,
                                                       String reason) {
        LocalDateTime endTime = startTime.plusMinutes(planHours.multiply(BigDecimal.valueOf(60)).longValue());
        return new ScheduleMachineAssignment(null, startTime, endTime, planHours, reason);
    }
}
