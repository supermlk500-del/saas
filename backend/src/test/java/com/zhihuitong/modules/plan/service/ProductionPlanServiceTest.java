package com.zhihuitong.modules.plan.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.plan.dto.ProductionPlanCreateRequest;
import com.zhihuitong.modules.plan.dto.ProductionPlanQuery;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.plan.vo.ProductionPlanListVo;
import com.zhihuitong.modules.process.entity.ProcessRoute;
import com.zhihuitong.modules.process.service.MachineService;
import com.zhihuitong.modules.process.service.ProcessRouteService;
import com.zhihuitong.modules.process.service.ProcessStepService;
import com.zhihuitong.modules.process.service.StepMachineCapabilityService;
import com.zhihuitong.security.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceTest {

    @Mock
    private ProductionPlanMapper productionPlanMapper;
    @Mock
    private PlanStepMapper planStepMapper;
    @Mock
    private BatchService batchService;
    @Mock
    private OrderService orderService;
    @Mock
    private ProcessRouteService processRouteService;
    @Mock
    private ProcessStepService processStepService;
    @Mock
    private MachineService machineService;
    @Mock
    private StepMachineCapabilityService capabilityService;
    @Mock
    private DataScopeService dataScopeService;

    private ProductionPlanService productionPlanService;

    @BeforeEach
    void setUp() {
        productionPlanService = new ProductionPlanService(
                productionPlanMapper,
                planStepMapper,
                batchService,
                orderService,
                processRouteService,
                processStepService,
                machineService,
                capabilityService,
                dataScopeService
        );
        org.mockito.Mockito.lenient().when(dataScopeService.apply(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listShouldExposeOrderAndProductFieldsInRows() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanId(2058721876820766721L);
        plan.setOrderId(101L);
        plan.setOrderItemId(201L);
        plan.setBatchId(301L);
        plan.setRouteId(401L);
        plan.setStatus("DRAFT");

        Page<ProductionPlan> page = new Page<>(1, 10);
        page.setRecords(List.of(plan));
        page.setTotal(1L);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(101L);
        orderInfo.setOrderNo("ORD-101");
        orderInfo.setCustomerName("星火纺织");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(201L);
        orderItem.setProductCode("P-201");
        orderItem.setProductName("弹力布");
        orderItem.setSpecification("210T");
        orderItem.setColor("藏青");

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setBatchId(301L);
        batchInfo.setBatchNo("B-301");

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(401L);
        route.setRouteName("染整路线");

        when(productionPlanMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(orderService.fetchOrderMap(any())).thenReturn(Map.of(101L, orderInfo));
        when(orderService.fetchOrderItemMap(any())).thenReturn(Map.of(201L, orderItem));
        when(batchService.requireBatch(301L)).thenReturn(batchInfo);
        when(processRouteService.requireRoute(401L)).thenReturn(route);

        TableDataInfo<ProductionPlanListVo> result = productionPlanService.list(new ProductionPlanQuery());

        assertThat(result.getRows()).hasSize(1);
        ProductionPlanListVo row = result.getRows().get(0);
        assertThat(row.getPlanId()).isEqualTo(2058721876820766721L);
        assertThat(row.getCustomerName()).isEqualTo("星火纺织");
        assertThat(row.getProductCode()).isEqualTo("P-201");
        assertThat(row.getProductName()).isEqualTo("弹力布");
        assertThat(row.getSpecification()).isEqualTo("210T");
        assertThat(row.getColor()).isEqualTo("藏青");
        assertThat(row.getBatchNo()).isEqualTo("B-301");
        assertThat(row.getRouteName()).isEqualTo("染整路线");
    }

    @Test
    void createShouldRejectDuplicateActiveOrderDrivenPlan() {
        ProductionPlan existingPlan = new ProductionPlan();
        existingPlan.setPlanId(999L);
        existingPlan.setOrderId(101L);
        existingPlan.setOrderItemId(201L);
        existingPlan.setBatchId(301L);
        existingPlan.setStatus("DRAFT");

        when(orderService.requireOrder(101L)).thenReturn(new OrderInfo());
        doNothing().when(orderService).validateOrderItemForOrder(101L, 201L);
        doNothing().when(orderService).ensureBatchAllocatedToOrderItem(101L, 201L, 301L);
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(existingPlan));

        assertThatThrownBy(() -> productionPlanService.create(buildCreateRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "message")
                .containsExactly(409, "该订单明细批次已存在活跃生产计划，请勿重复创建");

        verify(productionPlanMapper, never()).insert(any(ProductionPlan.class));
    }

    @Test
    void createShouldRejectBatchConflictWithOtherActivePlan() {
        ProductionPlan conflictPlan = new ProductionPlan();
        conflictPlan.setPlanId(888L);
        conflictPlan.setOrderId(999L);
        conflictPlan.setOrderItemId(777L);
        conflictPlan.setBatchId(301L);
        conflictPlan.setStatus("RUNNING");

        when(orderService.requireOrder(101L)).thenReturn(new OrderInfo());
        doNothing().when(orderService).validateOrderItemForOrder(101L, 201L);
        doNothing().when(orderService).ensureBatchAllocatedToOrderItem(101L, 201L, 301L);
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(conflictPlan));

        assertThatThrownBy(() -> productionPlanService.create(buildCreateRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code", "message")
                .containsExactly(409, "该批次已有活跃生产计划，计划ID：888，请先处理后再创建");

        verify(productionPlanMapper, never()).insert(any(ProductionPlan.class));
    }

    private ProductionPlanCreateRequest buildCreateRequest() {
        ProductionPlanCreateRequest request = new ProductionPlanCreateRequest();
        request.setOrderId(101L);
        request.setOrderItemId(201L);
        request.setBatchId(301L);
        request.setRouteId(401L);
        request.setPlanStartTime(LocalDateTime.of(2026, 5, 25, 8, 0));
        request.setPlanEndTime(LocalDateTime.of(2026, 5, 25, 18, 0));
        return request;
    }
}
