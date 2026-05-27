package com.zhihuitong.modules.plan.algorithm;

import com.zhihuitong.common.enums.DeviceStatus;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.service.MachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulingAlgorithmServiceTest {

    @Mock
    private MachineService machineService;

    private SchedulingAlgorithmService service;

    @BeforeEach
    void setUp() {
        service = new SchedulingAlgorithmService(machineService);
    }

    @Test
    void chooseEarliestFinishMachineShouldPreferFastestAvailableCandidate() {
        ProcessStep step = new ProcessStep();
        step.setDefaultHours(BigDecimal.valueOf(8));

        BatchInfo batch = new BatchInfo();
        batch.setWeight(BigDecimal.valueOf(100));

        StepMachineCapability slow = capability(11L, 20);
        StepMachineCapability fast = capability(22L, 50);
        when(machineService.requireMachine(11L)).thenReturn(machine(11L, "慢速机", DeviceStatus.IDLE.getCode()));
        when(machineService.requireMachine(22L)).thenReturn(machine(22L, "快速机", DeviceStatus.RUNNING.getCode()));

        LocalDateTime start = LocalDateTime.of(2026, 5, 28, 8, 0);
        ScheduleMachineAssignment result = service.chooseEarliestFinishMachine(step, batch, start, List.of(slow, fast));

        assertThat(result.getMachineId()).isEqualTo(22L);
        assertThat(result.getPlanHours()).isEqualByComparingTo("2.00");
        assertThat(result.getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 28, 10, 0));
        assertThat(result.getReason()).contains("greedy_eft", "快速机");
    }

    @Test
    void chooseEarliestFinishMachineShouldReturnUnassignedWhenNoCapabilityMatches() {
        ProcessStep step = new ProcessStep();
        step.setDefaultHours(BigDecimal.valueOf(3));

        LocalDateTime start = LocalDateTime.of(2026, 5, 28, 8, 0);
        ScheduleMachineAssignment result = service.chooseEarliestFinishMachine(step, new BatchInfo(), start, List.of());

        assertThat(result.getMachineId()).isNull();
        assertThat(result.getPlanHours()).isEqualByComparingTo("3");
        assertThat(result.getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 28, 11, 0));
        assertThat(result.getReason()).contains("未找到满足幅宽/批重");
    }

    private StepMachineCapability capability(Long machineId, int speed) {
        StepMachineCapability capability = new StepMachineCapability();
        capability.setMachineId(machineId);
        capability.setMaxSpeed(BigDecimal.valueOf(speed));
        return capability;
    }

    private Machine machine(Long machineId, String name, int status) {
        Machine machine = new Machine();
        machine.setMachineId(machineId);
        machine.setMachineName(name);
        machine.setMachineCode("M-" + machineId);
        machine.setStatus(status);
        return machine;
    }
}
