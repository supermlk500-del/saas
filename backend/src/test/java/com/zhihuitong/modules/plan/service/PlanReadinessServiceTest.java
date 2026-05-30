package com.zhihuitong.modules.plan.service;

import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.plan.vo.PlanReadinessVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanReadinessServiceTest {

    @Mock
    private ProductionPlanMapper productionPlanMapper;
    @Mock
    private PlanStepMapper planStepMapper;
    @Mock
    private OrderService orderService;

    private PlanReadinessService service;

    @BeforeEach
    void setUp() {
        service = new PlanReadinessService(productionPlanMapper, planStepMapper, orderService);
    }

    @Test
    void checkShouldReturnBlockersAndWarnings() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanId(101L);
        plan.setOrderId(501L);
        plan.setPlanEndTime(LocalDateTime.of(2026, 5, 31, 18, 0));

        OrderInfo order = new OrderInfo();
        order.setOrderId(501L);
        order.setDeliveryDate(LocalDateTime.of(2026, 5, 31, 12, 0));

        PlanStep unassigned = new PlanStep();
        unassigned.setPlanStepId(201L);
        unassigned.setPlanId(101L);
        unassigned.setStatus("PENDING");
        unassigned.setPlanStartTime(LocalDateTime.of(2026, 5, 31, 8, 0));
        unassigned.setPlanEndTime(LocalDateTime.of(2026, 5, 31, 10, 0));

        PlanStep abnormalMissingTime = new PlanStep();
        abnormalMissingTime.setPlanStepId(202L);
        abnormalMissingTime.setPlanId(101L);
        abnormalMissingTime.setMachineId(601L);
        abnormalMissingTime.setStatus("ABNORMAL");

        when(productionPlanMapper.selectById(101L)).thenReturn(plan);
        when(planStepMapper.selectList(any())).thenReturn(List.of(unassigned, abnormalMissingTime));
        when(orderService.requireOrder(501L)).thenReturn(order);

        PlanReadinessVo result = service.check(101L);

        assertThat(result.getReady()).isFalse();
        assertThat(result.getTotalStepCount()).isEqualTo(2);
        assertThat(result.getBlockerCount()).isEqualTo(3);
        assertThat(result.getWarningCount()).isEqualTo(1);
        assertThat(result.getIssues())
                .extracting("code")
                .containsExactly("MACHINE_UNASSIGNED", "TIME_WINDOW_MISSING", "STEP_ABNORMAL", "DELIVERY_DATE_RISK");
    }
}
