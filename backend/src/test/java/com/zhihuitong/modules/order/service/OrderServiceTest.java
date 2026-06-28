package com.zhihuitong.modules.order.service;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchResourcePoolService;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.batch.vo.BatchResourcePoolVo;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.order.dto.OrderSchedulePoolQuery;
import com.zhihuitong.modules.order.entity.OrderBatchLink;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.mapper.OrderBatchLinkMapper;
import com.zhihuitong.modules.order.mapper.OrderInfoMapper;
import com.zhihuitong.modules.order.mapper.OrderItemMapper;
import com.zhihuitong.modules.order.vo.OrderDetailVo;
import com.zhihuitong.modules.order.vo.OrderPlanSummaryVo;
import com.zhihuitong.modules.order.vo.OrderSchedulePoolVo;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.entity.ProcessRoute;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.entity.RouteStep;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.mapper.MachineMapper;
import com.zhihuitong.modules.process.mapper.ProcessRouteMapper;
import com.zhihuitong.modules.process.mapper.ProcessStepMapper;
import com.zhihuitong.modules.process.mapper.RouteStepMapper;
import com.zhihuitong.modules.process.mapper.StepMachineCapabilityMapper;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import com.zhihuitong.security.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderBatchLinkMapper orderBatchLinkMapper;
    @Mock
    private BatchService batchService;
    @Mock
    private BatchResourcePoolService batchResourcePoolService;
    @Mock
    private ProductionPlanMapper productionPlanMapper;
    @Mock
    private PlanStepMapper planStepMapper;
    @Mock
    private ProcessRouteMapper processRouteMapper;
    @Mock
    private ProcessStepMapper processStepMapper;
    @Mock
    private RouteStepMapper routeStepMapper;
    @Mock
    private StepMachineCapabilityMapper stepMachineCapabilityMapper;
    @Mock
    private MachineMapper machineMapper;
    @Mock
    private QcRecordMapper qcRecordMapper;
    @Mock
    private ExceptionRecordMapper exceptionRecordMapper;
    @Mock
    private DataScopeService dataScopeService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderInfoMapper,
                orderItemMapper,
                orderBatchLinkMapper,
                batchService,
                batchResourcePoolService,
                productionPlanMapper,
                planStepMapper,
                processRouteMapper,
                processStepMapper,
                routeStepMapper,
                stepMachineCapabilityMapper,
                machineMapper,
                qcRecordMapper,
                exceptionRecordMapper,
                dataScopeService
        );
        org.mockito.Mockito.lenient().when(dataScopeService.apply(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listOrderPlansShouldExposeOrderAndProductContext() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(101L);
        orderInfo.setOrderNo("ORD-101");
        orderInfo.setCustomerName("customer-a");

        ProductionPlan plan = new ProductionPlan();
        plan.setPlanId(2058721876820766721L);
        plan.setOrderId(101L);
        plan.setOrderItemId(201L);
        plan.setBatchId(301L);
        plan.setRouteId(401L);
        plan.setStatus("RELEASED");
        plan.setRemark("plan remark");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(201L);
        orderItem.setProductCode("P-201");
        orderItem.setProductName("fabric-a");
        orderItem.setSpecification("210T");
        orderItem.setColor("navy");

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setBatchId(301L);
        batchInfo.setBatchNo("B-301");

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(401L);
        route.setRouteName("route-a");

        when(orderInfoMapper.selectById(101L)).thenReturn(orderInfo);
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(plan));
        when(orderItemMapper.selectById(201L)).thenReturn(orderItem);
        when(batchService.requireBatch(301L)).thenReturn(batchInfo);
        when(processRouteMapper.selectById(401L)).thenReturn(route);

        List<OrderPlanSummaryVo> result = orderService.listOrderPlans(101L);

        assertThat(result).hasSize(1);
        OrderPlanSummaryVo summary = result.get(0);
        assertThat(summary.getPlanId()).isEqualTo(2058721876820766721L);
        assertThat(summary.getOrderId()).isEqualTo(101L);
        assertThat(summary.getOrderNo()).isEqualTo("ORD-101");
        assertThat(summary.getCustomerName()).isEqualTo("customer-a");
        assertThat(summary.getOrderItemId()).isEqualTo(201L);
        assertThat(summary.getProductCode()).isEqualTo("P-201");
        assertThat(summary.getProductName()).isEqualTo("fabric-a");
        assertThat(summary.getSpecification()).isEqualTo("210T");
        assertThat(summary.getColor()).isEqualTo("navy");
        assertThat(summary.getBatchId()).isEqualTo(301L);
        assertThat(summary.getBatchNo()).isEqualTo("B-301");
        assertThat(summary.getRouteId()).isEqualTo(401L);
        assertThat(summary.getRouteName()).isEqualTo("route-a");
        assertThat(summary.getStatus()).isEqualTo("RELEASED");
        assertThat(summary.getRemark()).isEqualTo("plan remark");
    }

    @Test
    void getDetailShouldExposeNestedPlanStepsAndQualitySummaryAlias() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(101L);
        orderInfo.setOrderNo("ORD-101");
        orderInfo.setCustomerName("customer-a");
        orderInfo.setStatus("READY");
        orderInfo.setOrderDate(LocalDateTime.of(2026, 5, 25, 8, 0));
        orderInfo.setDeliveryDate(LocalDateTime.of(2026, 5, 28, 18, 0));

        OrderItem item1 = new OrderItem();
        item1.setOrderItemId(201L);
        item1.setOrderId(101L);
        item1.setProductCode("P-201");
        item1.setProductName("fabric-a");
        item1.setQuantity(new BigDecimal("50"));
        item1.setRequiredWidth(new BigDecimal("160"));
        item1.setRequiredWeight(new BigDecimal("420"));

        OrderItem item2 = new OrderItem();
        item2.setOrderItemId(202L);
        item2.setOrderId(101L);
        item2.setProductCode("P-202");
        item2.setProductName("fabric-b");
        item2.setQuantity(new BigDecimal("30"));

        OrderBatchLink link1 = new OrderBatchLink();
        link1.setId(301L);
        link1.setOrderId(101L);
        link1.setOrderItemId(201L);
        link1.setBatchId(401L);
        link1.setAllocatedWeight(new BigDecimal("200"));
        link1.setAllocatedQuantity(new BigDecimal("20"));

        OrderBatchLink link2 = new OrderBatchLink();
        link2.setId(302L);
        link2.setOrderId(101L);
        link2.setOrderItemId(202L);
        link2.setBatchId(401L);
        link2.setAllocatedWeight(new BigDecimal("100"));
        link2.setAllocatedQuantity(new BigDecimal("10"));

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setBatchId(401L);
        batchInfo.setBatchNo("B-401");
        batchInfo.setSupplier("supplier-a");
        batchInfo.setWeight(new BigDecimal("600"));

        BatchResourcePoolVo batchResource = new BatchResourcePoolVo();
        batchResource.setBatchId(401L);
        batchResource.setRemainingWeight(new BigDecimal("300"));
        batchResource.setRemainingQuantity(new BigDecimal("50"));
        batchResource.setResourceStatus("PARTIALLY_ALLOCATED");
        batchResource.setResourceStatusLabel("Partial");
        batchResource.setReadyForSchedule(true);

        ProductionPlan plan = new ProductionPlan();
        plan.setPlanId(501L);
        plan.setOrderId(101L);
        plan.setOrderItemId(201L);
        plan.setBatchId(401L);
        plan.setRouteId(601L);
        plan.setStatus("RELEASED");

        PlanStep planStep = new PlanStep();
        planStep.setPlanStepId(701L);
        planStep.setPlanId(501L);
        planStep.setStepId(801L);
        planStep.setMachineId(901L);
        planStep.setPlanStartTime(LocalDateTime.of(2026, 5, 26, 8, 0));
        planStep.setPlanEndTime(LocalDateTime.of(2026, 5, 26, 12, 0));
        planStep.setPlanHours(new BigDecimal("4"));
        planStep.setSequenceNo(1);
        planStep.setStatus("PENDING");

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(601L);
        route.setRouteName("route-a");
        route.setDescription("route-desc");

        RouteStep routeStep = new RouteStep();
        routeStep.setRouteStepId(1001L);
        routeStep.setRouteId(601L);
        routeStep.setStepId(801L);
        routeStep.setSortOrder(1);
        routeStep.setIsMandatory(1);

        ProcessStep processStep = new ProcessStep();
        processStep.setStepId(801L);
        processStep.setStepCode("DYE");
        processStep.setStepName("Dye");

        Machine machine = new Machine();
        machine.setMachineId(901L);
        machine.setMachineCode("M-901");
        machine.setMachineName("machine-a");
        machine.setMachineType("dye");
        machine.setStatus(1);

        QcRecord qcRecord = new QcRecord();
        qcRecord.setInspectionId(1101L);
        qcRecord.setPlanStepId(701L);
        qcRecord.setQcItemId(1201L);
        qcRecord.setResultJudge("PASS");
        qcRecord.setResultValue("1.8");
        qcRecord.setInspectType("MANUAL");
        qcRecord.setInspectTime(LocalDateTime.of(2026, 5, 26, 12, 30));

        ExceptionRecord exceptionRecord = new ExceptionRecord();
        exceptionRecord.setExceptionId(1301L);
        exceptionRecord.setPlanStepId(701L);
        exceptionRecord.setExceptionType("TENSION");
        exceptionRecord.setExceptionLevel("P2");
        exceptionRecord.setDescription("tension drift");
        exceptionRecord.setStatus("OPEN");
        exceptionRecord.setCreateTime(LocalDateTime.of(2026, 5, 26, 13, 0));

        when(orderInfoMapper.selectById(101L)).thenReturn(orderInfo);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item1, item2));
        when(orderItemMapper.selectById(201L)).thenReturn(item1);
        when(orderBatchLinkMapper.selectList(any())).thenReturn(List.of(link1, link2));
        when(batchService.requireBatch(401L)).thenReturn(batchInfo);
        when(batchResourcePoolService.buildResourceMap(any())).thenReturn(Map.of(401L, batchResource));
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(plan));
        when(planStepMapper.selectList(any())).thenReturn(List.of(planStep));
        when(processRouteMapper.selectById(601L)).thenReturn(route);
        when(routeStepMapper.selectList(any())).thenReturn(List.of(routeStep));
        when(processStepMapper.selectById(801L)).thenReturn(processStep);
        when(machineMapper.selectById(901L)).thenReturn(machine);
        when(qcRecordMapper.selectList(any())).thenReturn(List.of(qcRecord));
        when(exceptionRecordMapper.selectList(any())).thenReturn(List.of(exceptionRecord));
        when(qcRecordMapper.selectCount(any())).thenReturn(1L);
        when(exceptionRecordMapper.selectCount(any())).thenReturn(1L);

        OrderDetailVo detail = orderService.getDetail(101L);

        assertThat(detail.getLinkedBatchCount()).isEqualTo(1L);
        assertThat(detail.getItems()).hasSize(2);
        assertThat(detail.getItems().get(0).getTargetWidth()).isEqualTo(new BigDecimal("160"));
        assertThat(detail.getPlanSummary()).hasSize(1);
        assertThat(detail.getPlanSummary().get(0).getPlanSteps()).hasSize(1);
        assertThat(detail.getPlanSummary().get(0).getPlanSteps().get(0).getStepName()).isEqualTo("Dye");
        assertThat(detail.getQualitySummary()).hasSize(1);
        assertThat(detail.getQualitySummary().get(0).getStepName()).isEqualTo("Dye");
        assertThat(detail.getQualitySummary().get(0).getBatchNo()).isEqualTo("B-401");
        assertThat(detail.getQualitySummary().get(0).getResultValue()).isEqualTo("1.8");
        assertThat(detail.getExceptionSummary()).hasSize(1);
        assertThat(detail.getExceptionSummary().get(0).getDescription()).isEqualTo("tension drift");
    }

    @Test
    void listSchedulePoolShouldExposeDirectAggregationFieldsAndBlockingReason() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(101L);
        orderInfo.setOrderNo("ORD-101");
        orderInfo.setCustomerName("customer-a");
        orderInfo.setStatus("READY");
        orderInfo.setOrderDate(LocalDateTime.of(2026, 5, 25, 8, 0));
        orderInfo.setDeliveryDate(LocalDateTime.of(2026, 5, 30, 18, 0));

        OrderItem item = new OrderItem();
        item.setOrderItemId(201L);
        item.setOrderId(101L);
        item.setProductCode("P-201");
        item.setProductName("fabric-a");
        item.setSpecification("210T");
        item.setColor("black");
        item.setQuantity(new BigDecimal("50"));
        item.setRequiredWidth(new BigDecimal("160"));
        item.setRequiredWeight(new BigDecimal("420"));

        OrderBatchLink link = new OrderBatchLink();
        link.setId(301L);
        link.setOrderId(101L);
        link.setOrderItemId(201L);
        link.setBatchId(401L);
        link.setAllocatedWeight(new BigDecimal("200"));
        link.setAllocatedQuantity(new BigDecimal("50"));

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setBatchId(401L);
        batchInfo.setBatchNo("B-401");
        batchInfo.setSupplier("supplier-a");
        batchInfo.setWeight(new BigDecimal("600"));
        batchInfo.setWidth(new BigDecimal("170"));
        batchInfo.setComposition("cotton");

        BatchResourcePoolVo batchResource = new BatchResourcePoolVo();
        batchResource.setBatchId(401L);
        batchResource.setRemainingWeight(new BigDecimal("400"));
        batchResource.setRemainingQuantity(BigDecimal.ZERO);
        batchResource.setResourceStatus("ALLOCATED");
        batchResource.setResourceStatusLabel("Allocated");
        batchResource.setReadyForSchedule(true);

        ProductionPlan activePlan = new ProductionPlan();
        activePlan.setPlanId(501L);
        activePlan.setOrderId(101L);
        activePlan.setOrderItemId(201L);
        activePlan.setBatchId(401L);
        activePlan.setStatus("DRAFT");

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(601L);
        route.setRouteName("route-a");
        route.setDescription("route-desc");
        route.setIsActive(1);

        RouteStep routeStep = new RouteStep();
        routeStep.setRouteStepId(701L);
        routeStep.setRouteId(601L);
        routeStep.setStepId(801L);
        routeStep.setSortOrder(1);
        routeStep.setIsMandatory(1);

        ProcessStep processStep = new ProcessStep();
        processStep.setStepId(801L);
        processStep.setStepCode("DYE");
        processStep.setStepName("Dye");

        StepMachineCapability capability = new StepMachineCapability();
        capability.setCapId(901L);
        capability.setStepId(801L);
        capability.setMachineId(1001L);
        capability.setIsActive(1);
        capability.setMinWidth(new BigDecimal("100"));
        capability.setMaxWidth(new BigDecimal("200"));
        capability.setMaxBatchWeight(new BigDecimal("1000"));

        Machine machine = new Machine();
        machine.setMachineId(1001L);
        machine.setMachineCode("M-1001");
        machine.setMachineName("machine-a");
        machine.setMachineType("dye");
        machine.setStatus(1);

        when(orderInfoMapper.selectList(any())).thenReturn(List.of(orderInfo));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(orderBatchLinkMapper.selectList(any())).thenReturn(List.of(link));
        when(batchService.requireBatch(401L)).thenReturn(batchInfo);
        when(batchResourcePoolService.buildResourceMap(any())).thenReturn(Map.of(401L, batchResource));
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(activePlan));
        when(processRouteMapper.selectList(any())).thenReturn(List.of(route));
        when(routeStepMapper.selectList(any())).thenReturn(List.of(routeStep));
        when(processStepMapper.selectById(801L)).thenReturn(processStep);
        when(stepMachineCapabilityMapper.selectList(any())).thenReturn(List.of(capability));
        when(machineMapper.selectById(1001L)).thenReturn(machine);

        TableDataInfo<OrderSchedulePoolVo> table = orderService.listSchedulePool(new OrderSchedulePoolQuery());

        assertThat(table.getTotal()).isEqualTo(1L);
        assertThat(table.getRows()).hasSize(1);
        OrderSchedulePoolVo row = table.getRows().get(0);
        assertThat(row.getOrderItemId()).isEqualTo(201L);
        assertThat(row.getLinkedBatches()).hasSize(1);
        assertThat(row.getAllocatedQuantity()).isEqualByComparingTo("50");
        assertThat(row.getRecommendedRoute()).isNotNull();
        assertThat(row.getRecommendedRoute().getRouteName()).isEqualTo("route-a");
        assertThat(row.getRecommendedMachineList()).hasSize(1);
        assertThat(row.getRecommendedMachineList().get(0).getMachineCode()).isEqualTo("M-1001");
        assertThat(row.getActivePlanCount()).isEqualTo(1L);
        assertThat(row.isReadyForSchedule()).isFalse();
        assertThat(row.getScheduleBlockedReason()).contains("active production plan");
    }

    @Test
    void deleteShouldRemoveCancelledPlansAndOrder() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(101L);

        ProductionPlan cancelledPlan = new ProductionPlan();
        cancelledPlan.setPlanId(501L);
        cancelledPlan.setOrderId(101L);
        cancelledPlan.setStatus("CANCELLED");

        PlanStep planStep = new PlanStep();
        planStep.setPlanStepId(701L);
        planStep.setPlanId(501L);

        when(orderInfoMapper.selectById(101L)).thenReturn(orderInfo);
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(cancelledPlan));
        when(planStepMapper.selectList(any())).thenReturn(List.of(planStep));
        when(qcRecordMapper.selectCount(any())).thenReturn(0L);
        when(exceptionRecordMapper.selectCount(any())).thenReturn(0L);

        orderService.delete(101L);

        verify(planStepMapper).delete(any());
        verify(productionPlanMapper).delete(any());
        verify(orderBatchLinkMapper).delete(any());
        verify(orderItemMapper).delete(any());
        verify(orderInfoMapper).deleteById(101L);
    }

    @Test
    void deleteShouldRejectActiveOrCompletedPlans() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(101L);

        ProductionPlan completedPlan = new ProductionPlan();
        completedPlan.setPlanId(501L);
        completedPlan.setOrderId(101L);
        completedPlan.setStatus("COMPLETED");

        when(orderInfoMapper.selectById(101L)).thenReturn(orderInfo);
        when(productionPlanMapper.selectList(any())).thenReturn(List.of(completedPlan));

        assertThatThrownBy(() -> orderService.delete(101L))
                .hasMessageContaining("active or completed production plans");

        verify(planStepMapper, never()).delete(any());
        verify(productionPlanMapper, never()).delete(any());
        verify(orderInfoMapper, never()).deleteById(101L);
    }
}
