package com.zhihuitong.modules.process.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.mapper.ProcessStepMapper;
import com.zhihuitong.modules.process.mapper.RouteStepMapper;
import com.zhihuitong.modules.process.mapper.StepMachineCapabilityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessStepServiceTest {

    private ProcessStepMapper processStepMapper;
    private RouteStepMapper routeStepMapper;
    private StepMachineCapabilityMapper capabilityMapper;
    private PlanStepMapper planStepMapper;
    private ProcessStepService service;

    @BeforeEach
    void setUp() {
        processStepMapper = mock(ProcessStepMapper.class);
        routeStepMapper = mock(RouteStepMapper.class);
        capabilityMapper = mock(StepMachineCapabilityMapper.class);
        planStepMapper = mock(PlanStepMapper.class);
        service = new ProcessStepService(processStepMapper, routeStepMapper, capabilityMapper, planStepMapper);

        ProcessStep step = new ProcessStep();
        step.setStepId(1L);
        when(processStepMapper.selectById(1L)).thenReturn(step);
    }

    @Test
    void deleteRemovesUnreferencedStep() {
        when(routeStepMapper.selectCount(any())).thenReturn(0L);
        when(capabilityMapper.selectCount(any())).thenReturn(0L);
        when(planStepMapper.selectCount(any())).thenReturn(0L);

        service.delete(1L);

        verify(processStepMapper).deleteById(1L);
    }

    @Test
    void deleteRejectsStepReferencedByRoute() {
        when(routeStepMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.delete(1L));

        verify(processStepMapper, never()).deleteById(1L);
    }
}
