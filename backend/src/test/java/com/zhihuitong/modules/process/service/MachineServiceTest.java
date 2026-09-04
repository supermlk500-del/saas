package com.zhihuitong.modules.process.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.mapper.MachineMapper;
import com.zhihuitong.modules.process.mapper.StepMachineCapabilityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    private MachineMapper machineMapper;

    @Mock
    private StepMachineCapabilityMapper stepMachineCapabilityMapper;

    @Mock
    private PlanStepMapper planStepMapper;

    private MachineService service;

    @BeforeEach
    void setUp() {
        service = new MachineService(machineMapper, stepMachineCapabilityMapper, planStepMapper);
    }

    @Test
    void delete_removesUnreferencedMachine() {
        when(machineMapper.selectById(1L)).thenReturn(machine());
        when(stepMachineCapabilityMapper.selectCount(any())).thenReturn(0L);
        when(planStepMapper.selectCount(any())).thenReturn(0L);

        service.delete(1L);

        verify(machineMapper).deleteById(1L);
    }

    @Test
    void delete_rejectsMachineReferencedByCapability() {
        when(machineMapper.selectById(1L)).thenReturn(machine());
        when(stepMachineCapabilityMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("process capabilities");

        verify(machineMapper, never()).deleteById(1L);
        verify(planStepMapper, never()).selectCount(any());
    }

    @Test
    void delete_rejectsMachineReferencedByPlanStep() {
        when(machineMapper.selectById(1L)).thenReturn(machine());
        when(stepMachineCapabilityMapper.selectCount(any())).thenReturn(0L);
        when(planStepMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("production plan steps");

        verify(machineMapper, never()).deleteById(1L);
    }

    private Machine machine() {
        Machine machine = new Machine();
        machine.setMachineId(1L);
        machine.setMachineCode("M-001");
        machine.setMachineName("Test machine");
        return machine;
    }
}
