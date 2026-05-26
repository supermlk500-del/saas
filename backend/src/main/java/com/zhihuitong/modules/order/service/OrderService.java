package com.zhihuitong.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchResourcePoolService;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.batch.vo.BatchResourcePoolVo;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.order.dto.OrderBatchLinkUpsertRequest;
import com.zhihuitong.modules.order.dto.OrderItemUpsertRequest;
import com.zhihuitong.modules.order.dto.OrderQuery;
import com.zhihuitong.modules.order.dto.OrderSchedulePoolQuery;
import com.zhihuitong.modules.order.dto.OrderStatusPatchRequest;
import com.zhihuitong.modules.order.dto.OrderUpsertRequest;
import com.zhihuitong.modules.order.entity.OrderBatchLink;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.mapper.OrderBatchLinkMapper;
import com.zhihuitong.modules.order.mapper.OrderInfoMapper;
import com.zhihuitong.modules.order.mapper.OrderItemMapper;
import com.zhihuitong.modules.order.vo.OrderBatchLinkVo;
import com.zhihuitong.modules.order.vo.OrderDetailVo;
import com.zhihuitong.modules.order.vo.OrderExceptionSummaryVo;
import com.zhihuitong.modules.order.vo.OrderItemVo;
import com.zhihuitong.modules.order.vo.OrderListVo;
import com.zhihuitong.modules.order.vo.OrderMachineSummaryVo;
import com.zhihuitong.modules.order.vo.OrderMachineOccupiedRangeVo;
import com.zhihuitong.modules.order.vo.OrderPlanStepSummaryVo;
import com.zhihuitong.modules.order.vo.OrderPlanSummaryVo;
import com.zhihuitong.modules.order.vo.OrderQcSummaryVo;
import com.zhihuitong.modules.order.vo.OrderRouteSummaryVo;
import com.zhihuitong.modules.order.vo.OrderRouteStepVo;
import com.zhihuitong.modules.order.vo.OrderSchedulePoolMachineVo;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Set<String> ORDER_STATUSES = Set.of("NEW", "READY", "PLANNING", "IN_PROGRESS", "DONE", "CANCELLED");
    private static final Set<String> ACTIVE_PLAN_STATUSES = Set.of("DRAFT", "RELEASED", "RUNNING");

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderBatchLinkMapper orderBatchLinkMapper;
    private final BatchService batchService;
    private final BatchResourcePoolService batchResourcePoolService;
    private final ProductionPlanMapper productionPlanMapper;
    private final PlanStepMapper planStepMapper;
    private final ProcessRouteMapper processRouteMapper;
    private final ProcessStepMapper processStepMapper;
    private final RouteStepMapper routeStepMapper;
    private final StepMachineCapabilityMapper stepMachineCapabilityMapper;
    private final MachineMapper machineMapper;
    private final QcRecordMapper qcRecordMapper;
    private final ExceptionRecordMapper exceptionRecordMapper;

    public OrderService(OrderInfoMapper orderInfoMapper,
                        OrderItemMapper orderItemMapper,
                        OrderBatchLinkMapper orderBatchLinkMapper,
                        BatchService batchService,
                        BatchResourcePoolService batchResourcePoolService,
                        ProductionPlanMapper productionPlanMapper,
                        PlanStepMapper planStepMapper,
                        ProcessRouteMapper processRouteMapper,
                        ProcessStepMapper processStepMapper,
                        RouteStepMapper routeStepMapper,
                        StepMachineCapabilityMapper stepMachineCapabilityMapper,
                        MachineMapper machineMapper,
                        QcRecordMapper qcRecordMapper,
                        ExceptionRecordMapper exceptionRecordMapper) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderBatchLinkMapper = orderBatchLinkMapper;
        this.batchService = batchService;
        this.batchResourcePoolService = batchResourcePoolService;
        this.productionPlanMapper = productionPlanMapper;
        this.planStepMapper = planStepMapper;
        this.processRouteMapper = processRouteMapper;
        this.processStepMapper = processStepMapper;
        this.routeStepMapper = routeStepMapper;
        this.stepMachineCapabilityMapper = stepMachineCapabilityMapper;
        this.machineMapper = machineMapper;
        this.qcRecordMapper = qcRecordMapper;
        this.exceptionRecordMapper = exceptionRecordMapper;
    }

    public TableDataInfo<OrderListVo> list(OrderQuery query) {
        validateTimeRange(query.getDeliveryDateFrom(), query.getDeliveryDateTo(), "deliveryDateFrom must be earlier than or equal to deliveryDateTo");
        Page<OrderInfo> page = orderInfoMapper.selectPage(query.toPage(), Wrappers.<OrderInfo>lambdaQuery()
                .like(StringUtils.hasText(query.getOrderNo()), OrderInfo::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getCustomerName()), OrderInfo::getCustomerName, query.getCustomerName())
                .eq(StringUtils.hasText(query.getStatus()), OrderInfo::getStatus, query.getStatus())
                .ge(query.getDeliveryDateFrom() != null, OrderInfo::getDeliveryDate, query.getDeliveryDateFrom())
                .le(query.getDeliveryDateTo() != null, OrderInfo::getDeliveryDate, query.getDeliveryDateTo())
                .orderByDesc(OrderInfo::getCreateTime));
        List<OrderInfo> records = page.getRecords();
        List<Long> orderIds = records.stream().map(OrderInfo::getOrderId).toList();
        Map<Long, Long> batchCountMap = aggregateBatchCounts(orderIds);
        Map<Long, Long> planCountMap = aggregatePlanCounts(orderIds);
        List<OrderListVo> rows = records.stream().map(order -> toListVo(order, batchCountMap, planCountMap)).toList();
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public OrderDetailVo getDetail(Long orderId) {
        OrderInfo order = requireOrder(orderId);
        List<OrderItem> items = findOrderItems(orderId);
        List<OrderBatchLinkVo> linkedBatches = listOrderBatches(orderId);
        List<ProductionPlan> plans = findOrderPlans(orderId);
        List<PlanStep> planSteps = findPlanStepsByPlans(plans);
        List<OrderPlanStepSummaryVo> planStepSummary = buildPlanStepSummary(planSteps);
        List<OrderPlanSummaryVo> planSummary = buildPlanSummary(plans);
        Map<Long, List<OrderPlanStepSummaryVo>> planStepsByPlan = planStepSummary.stream()
                .collect(Collectors.groupingBy(OrderPlanStepSummaryVo::getPlanId, LinkedHashMap::new, Collectors.toList()));
        planSummary.forEach(summary -> summary.setPlanSteps(planStepsByPlan.getOrDefault(summary.getPlanId(), Collections.emptyList())));
        Map<Long, String> stepNameByPlanStepId = planStepSummary.stream()
                .collect(Collectors.toMap(OrderPlanStepSummaryVo::getPlanStepId, OrderPlanStepSummaryVo::getStepName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> batchNoByPlanId = planSummary.stream()
                .collect(Collectors.toMap(OrderPlanSummaryVo::getPlanId, OrderPlanSummaryVo::getBatchNo, (left, right) -> left, LinkedHashMap::new));
        List<OrderQcSummaryVo> qcSummary = buildQcSummary(planSteps, stepNameByPlanStepId, batchNoByPlanId);
        List<OrderExceptionSummaryVo> exceptionSummary = buildExceptionSummary(planSteps, stepNameByPlanStepId, batchNoByPlanId);

        OrderDetailVo detail = new OrderDetailVo();
        detail.setOrderId(order.getOrderId());
        detail.setOrderNo(order.getOrderNo());
        detail.setCustomerName(order.getCustomerName());
        detail.setOrderDate(order.getOrderDate());
        detail.setDeliveryDate(order.getDeliveryDate());
        detail.setPriority(order.getPriority());
        detail.setStatus(order.getStatus());
        detail.setRemark(order.getRemark());
        detail.setCreateTime(order.getCreateTime());
        detail.setLinkedBatchCount(linkedBatches.stream().map(OrderBatchLinkVo::getBatchId).filter(Objects::nonNull).distinct().count());
        detail.setGeneratedPlanCount(planSummary.size());
        detail.setQcRecordCount(countOrderQcRecords(planSteps));
        detail.setExceptionCount(countOrderExceptions(planSteps));
        detail.setItems(items.stream().map(this::toItemVo).toList());
        detail.setLinkedBatches(linkedBatches);
        detail.setPlanSummary(planSummary);
        detail.setPlanSteps(planStepSummary);
        detail.setRouteSummary(buildRouteSummary(plans));
        detail.setMachineSummary(buildMachineSummary(planSteps));
        detail.setQcSummary(qcSummary);
        detail.setQualitySummary(qcSummary);
        detail.setLatestQcRecord(qcSummary.isEmpty() ? null : qcSummary.get(0));
        detail.setExceptionSummary(exceptionSummary);
        detail.setLatestException(exceptionSummary.isEmpty() ? null : exceptionSummary.get(0));
        return detail;
    }

    public List<OrderPlanSummaryVo> listOrderPlans(Long orderId) {
        requireOrder(orderId);
        return buildPlanSummary(findOrderPlans(orderId));
    }

    public List<OrderRouteSummaryVo> listOrderRoutes(Long orderId) {
        requireOrder(orderId);
        return buildRouteSummary(findOrderPlans(orderId));
    }

    public List<OrderMachineSummaryVo> listOrderMachines(Long orderId) {
        requireOrder(orderId);
        return buildMachineSummary(findPlanStepsByPlans(findOrderPlans(orderId)));
    }

    public List<OrderQcSummaryVo> listOrderQcRecords(Long orderId) {
        requireOrder(orderId);
        return buildQcSummary(findPlanStepsByPlans(findOrderPlans(orderId)), Collections.emptyMap(), Collections.emptyMap());
    }

    public List<OrderExceptionSummaryVo> listOrderExceptions(Long orderId) {
        requireOrder(orderId);
        return buildExceptionSummary(findPlanStepsByPlans(findOrderPlans(orderId)), Collections.emptyMap(), Collections.emptyMap());
    }

    public TableDataInfo<OrderSchedulePoolVo> listSchedulePool(OrderSchedulePoolQuery query) {
        List<OrderInfo> orders = orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .like(StringUtils.hasText(query.getOrderNo()), OrderInfo::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getCustomerName()), OrderInfo::getCustomerName, query.getCustomerName())
                .eq(StringUtils.hasText(query.getStatus()), OrderInfo::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPriority()), OrderInfo::getPriority, query.getPriority())
                .orderByDesc(OrderInfo::getDeliveryDate)
                .orderByDesc(OrderInfo::getCreateTime));
        if (orders.isEmpty()) {
            return TableDataInfoBuilder.build(Collections.emptyList(), 0L);
        }
        List<Long> orderIds = orders.stream().map(OrderInfo::getOrderId).toList();
        Map<Long, OrderInfo> orderMap = orders.stream()
                .collect(Collectors.toMap(OrderInfo::getOrderId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> orderRank = new LinkedHashMap<>();
        for (int index = 0; index < orders.size(); index++) {
            orderRank.put(orders.get(index).getOrderId(), index);
        }
        List<OrderItem> items = new ArrayList<>(orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getOrderItemId)));
        items.sort(Comparator
                .comparingInt((OrderItem item) -> orderRank.getOrDefault(item.getOrderId(), Integer.MAX_VALUE))
                .thenComparing(OrderItem::getOrderItemId, Comparator.nullsLast(Comparator.naturalOrder())));
        List<OrderBatchLink> links = orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                .in(OrderBatchLink::getOrderId, orderIds));
        Map<Long, List<OrderBatchLink>> linksByOrderItem = links.stream().collect(Collectors.groupingBy(OrderBatchLink::getOrderItemId));
        Map<Long, BatchInfo> batchMap = links.stream()
                .map(OrderBatchLink::getBatchId)
                .distinct()
                .map(batchService::requireBatch)
                .collect(Collectors.toMap(BatchInfo::getBatchId, Function.identity()));
        Map<Long, BatchResourcePoolVo> batchResourceMap = batchResourcePoolService.buildResourceMap(batchMap.keySet());
        List<ProductionPlan> plans = productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .in(ProductionPlan::getOrderId, orderIds)
                .orderByDesc(ProductionPlan::getCreateTime));
        Map<Long, List<ProductionPlan>> plansByOrderItem = plans.stream()
                .filter(plan -> plan.getOrderItemId() != null)
                .collect(Collectors.groupingBy(ProductionPlan::getOrderItemId));

        List<ProcessRoute> activeRoutes = processRouteMapper.selectList(Wrappers.<ProcessRoute>lambdaQuery()
                .eq(ProcessRoute::getIsActive, 1)
                .orderByAsc(ProcessRoute::getCreateTime));
        Map<Long, List<RouteStep>> routeStepsByRoute = routeStepMapper.selectList(Wrappers.<RouteStep>lambdaQuery()
                        .in(!activeRoutes.isEmpty(), RouteStep::getRouteId, activeRoutes.stream().map(ProcessRoute::getRouteId).toList())
                        .orderByAsc(RouteStep::getRouteId)
                        .orderByAsc(RouteStep::getSortOrder))
                .stream().collect(Collectors.groupingBy(RouteStep::getRouteId));
        List<StepMachineCapability> allCapabilities = stepMachineCapabilityMapper.selectList(Wrappers.<StepMachineCapability>lambdaQuery()
                .eq(StepMachineCapability::getIsActive, 1));
        Map<Long, List<StepMachineCapability>> capabilitiesByStep = allCapabilities.stream().collect(Collectors.groupingBy(StepMachineCapability::getStepId));
        Map<Long, ProcessStep> routeProcessStepMap = routeStepsByRoute.values().stream()
                .flatMap(List::stream)
                .map(RouteStep::getStepId)
                .filter(Objects::nonNull)
                .distinct()
                .map(processStepMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
        Map<Long, Machine> machineMap = allCapabilities.stream()
                .map(StepMachineCapability::getMachineId)
                .filter(Objects::nonNull)
                .distinct()
                .map(machineMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Machine::getMachineId, Function.identity()));

        List<OrderSchedulePoolVo> rows = items.stream()
                .map(item -> toSchedulePoolVo(
                        orderMap.get(item.getOrderId()),
                        item,
                        linksByOrderItem.getOrDefault(item.getOrderItemId(), Collections.emptyList()),
                        batchMap,
                        batchResourceMap,
                        plansByOrderItem.getOrDefault(item.getOrderItemId(), Collections.emptyList()),
                        activeRoutes,
                        routeStepsByRoute,
                        routeProcessStepMap,
                        capabilitiesByStep,
                        machineMap
                ))
                .filter(vo -> query.getReadyForSchedule() == null || vo.isReadyForSchedule() == query.getReadyForSchedule())
                .toList();
        long total = rows.size();
        long offset = Math.max(query.getPageNum() - 1, 0) * query.getPageSize();
        int fromIndex = (int) Math.min(offset, total);
        int toIndex = (int) Math.min(offset + query.getPageSize(), total);
        return TableDataInfoBuilder.build(rows.subList(fromIndex, toIndex), total);
    }

    @Transactional
    public OrderInfo create(OrderUpsertRequest request) {
        validateOrderDates(request.getOrderDate(), request.getDeliveryDate());
        assertOrderStatus(request.getStatus());
        checkOrderNoUnique(request.getOrderNo(), null);
        OrderInfo entity = new OrderInfo();
        copyOrderRequest(request, entity);
        entity.setCreateTime(LocalDateTime.now());
        orderInfoMapper.insert(entity);
        return entity;
    }

    @Transactional
    public OrderInfo update(Long orderId, OrderUpsertRequest request) {
        validateOrderDates(request.getOrderDate(), request.getDeliveryDate());
        assertOrderStatus(request.getStatus());
        OrderInfo entity = requireOrder(orderId);
        checkOrderNoUnique(request.getOrderNo(), orderId);
        copyOrderRequest(request, entity);
        orderInfoMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public OrderInfo patchStatus(Long orderId, OrderStatusPatchRequest request) {
        assertOrderStatus(request.getStatus());
        OrderInfo entity = requireOrder(orderId);
        entity.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getReason())) {
            entity.setRemark(AuditRemarkUtils.append(entity.getRemark(), "ORDER_STATUS", request.getReason()));
        }
        orderInfoMapper.updateById(entity);
        return entity;
    }

    public List<OrderItemVo> listItems(Long orderId) {
        requireOrder(orderId);
        return findOrderItems(orderId).stream().map(this::toItemVo).toList();
    }

    @Transactional
    public OrderItem createItem(Long orderId, OrderItemUpsertRequest request) {
        requireOrder(orderId);
        validateOrderItemRequest(request);
        OrderItem item = new OrderItem();
        copyOrderItemRequest(orderId, request, item);
        orderItemMapper.insert(item);
        return item;
    }

    @Transactional
    public OrderItem updateItem(Long orderId, Long orderItemId, OrderItemUpsertRequest request) {
        requireOrder(orderId);
        validateOrderItemRequest(request);
        OrderItem item = requireOrderItem(orderItemId);
        if (!orderId.equals(item.getOrderId())) {
            throw new BusinessException(404, "Order item not found");
        }
        copyOrderItemRequest(orderId, request, item);
        orderItemMapper.updateById(item);
        return item;
    }

    @Transactional
    public void deleteItem(Long orderId, Long orderItemId) {
        requireOrder(orderId);
        OrderItem item = requireOrderItem(orderItemId);
        if (!orderId.equals(item.getOrderId())) {
            throw new BusinessException(404, "Order item not found");
        }
        long linkCount = orderBatchLinkMapper.selectCount(Wrappers.<OrderBatchLink>lambdaQuery()
                .eq(OrderBatchLink::getOrderItemId, orderItemId));
        if (linkCount > 0) {
            throw new BusinessException(409, "Order item is already linked to batches and cannot be deleted");
        }
        long planCount = productionPlanMapper.selectCount(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getOrderItemId, orderItemId));
        if (planCount > 0) {
            throw new BusinessException(409, "Order item is already referenced by production plans and cannot be deleted");
        }
        orderItemMapper.deleteById(orderItemId);
    }

    public List<OrderBatchLinkVo> listOrderBatches(Long orderId) {
        requireOrder(orderId);
        List<OrderBatchLink> links = orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                .eq(OrderBatchLink::getOrderId, orderId)
                .orderByAsc(OrderBatchLink::getOrderItemId)
                .orderByAsc(OrderBatchLink::getBatchId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BatchInfo> batchMap = links.stream()
                .map(OrderBatchLink::getBatchId)
                .distinct()
                .map(batchService::requireBatch)
                .collect(Collectors.toMap(BatchInfo::getBatchId, Function.identity()));
        Map<Long, BatchResourcePoolVo> resourceMap = batchResourcePoolService.buildResourceMap(batchMap.keySet());
        return links.stream()
                .map(link -> toBatchLinkVo(link, batchMap.get(link.getBatchId()), resourceMap.get(link.getBatchId())))
                .toList();
    }

    @Transactional
    public OrderBatchLink createBatchLink(Long orderId, OrderBatchLinkUpsertRequest request) {
        requireOrder(orderId);
        OrderItem item = requireOrderItem(request.getOrderItemId());
        if (!orderId.equals(item.getOrderId())) {
            throw new BusinessException(422, "Order item does not belong to the target order");
        }
        BatchInfo batch = batchService.requireBatch(request.getBatchId());
        validateOrderBatchLinkRequest(request, item, batch);
        checkBatchLinkUnique(orderId, request.getOrderItemId(), request.getBatchId(), null);
        OrderBatchLink link = new OrderBatchLink();
        copyBatchLinkRequest(orderId, request, link);
        orderBatchLinkMapper.insert(link);
        return link;
    }

    @Transactional
    public void deleteBatchLink(Long orderId, Long linkId) {
        requireOrder(orderId);
        OrderBatchLink link = requireBatchLink(linkId);
        if (!orderId.equals(link.getOrderId())) {
            throw new BusinessException(404, "Order batch link not found");
        }
        long planCount = productionPlanMapper.selectCount(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getOrderId, orderId)
                .eq(ProductionPlan::getOrderItemId, link.getOrderItemId())
                .eq(ProductionPlan::getBatchId, link.getBatchId()));
        if (planCount > 0) {
            throw new BusinessException(409, "Order batch link is already referenced by production plans and cannot be deleted");
        }
        orderBatchLinkMapper.deleteById(linkId);
    }

    public OrderInfo requireOrder(Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    public OrderItem requireOrderItem(Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException(404, "订单明细不存在");
        }
        return item;
    }

    public void validateOrderItemForOrder(Long orderId, Long orderItemId) {
        OrderItem item = requireOrderItem(orderItemId);
        if (!orderId.equals(item.getOrderId())) {
            throw new BusinessException(422, "订单明细不属于当前订单");
        }
    }

    public void ensureBatchAllocatedToOrderItem(Long orderId, Long orderItemId, Long batchId) {
        long count = orderBatchLinkMapper.selectCount(Wrappers.<OrderBatchLink>lambdaQuery()
                .eq(OrderBatchLink::getOrderId, orderId)
                .eq(OrderBatchLink::getOrderItemId, orderItemId)
                .eq(OrderBatchLink::getBatchId, batchId));
        if (count <= 0) {
            throw new BusinessException(422, "该批次尚未分配给当前订单明细");
        }
    }

    public Map<Long, OrderInfo> fetchOrderMap(List<Long> orderIds) {
        return orderIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(this::requireOrder)
                .collect(Collectors.toMap(OrderInfo::getOrderId, Function.identity()));
    }

    public Map<Long, OrderItem> fetchOrderItemMap(List<Long> orderItemIds) {
        return orderItemIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(this::requireOrderItem)
                .collect(Collectors.toMap(OrderItem::getOrderItemId, Function.identity()));
    }

    private Map<Long, Long> aggregateBatchCounts(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                        .in(OrderBatchLink::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(OrderBatchLink::getOrderId, Collectors.mapping(OrderBatchLink::getBatchId, Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));
    }

    private Map<Long, Long> aggregatePlanCounts(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                        .in(ProductionPlan::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(ProductionPlan::getOrderId, Collectors.counting()));
    }

    private List<OrderItem> findOrderItems(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getOrderItemId));
    }

    private List<ProductionPlan> findOrderPlans(Long orderId) {
        return productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getOrderId, orderId)
                .orderByDesc(ProductionPlan::getCreateTime));
    }

    private List<PlanStep> findPlanStepsByPlans(List<ProductionPlan> plans) {
        if (plans.isEmpty()) {
            return Collections.emptyList();
        }
        return planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                .in(PlanStep::getPlanId, plans.stream().map(ProductionPlan::getPlanId).toList())
                .orderByAsc(PlanStep::getPlanId)
                .orderByAsc(PlanStep::getSequenceNo));
    }

    private List<OrderPlanSummaryVo> buildPlanSummary(List<ProductionPlan> plans) {
        if (plans.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, OrderInfo> orderMap = fetchOrderMap(plans.stream().map(ProductionPlan::getOrderId).toList());
        Map<Long, OrderItem> orderItemMap = fetchOrderItemMap(plans.stream().map(ProductionPlan::getOrderItemId).toList());
        Map<Long, BatchInfo> batchMap = plans.stream()
                .map(ProductionPlan::getBatchId)
                .filter(Objects::nonNull)
                .distinct()
                .map(batchService::requireBatch)
                .collect(Collectors.toMap(BatchInfo::getBatchId, Function.identity()));
        Map<Long, ProcessRoute> routeMap = plans.stream()
                .map(ProductionPlan::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .map(routeId -> processRouteMapper.selectById(routeId))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProcessRoute::getRouteId, Function.identity()));
        return plans.stream().map(plan -> {
            OrderPlanSummaryVo vo = new OrderPlanSummaryVo();
            vo.setPlanId(plan.getPlanId());
            vo.setOrderId(plan.getOrderId());
            OrderInfo order = orderMap.get(plan.getOrderId());
            vo.setOrderNo(order == null ? null : order.getOrderNo());
            vo.setCustomerName(order == null ? null : order.getCustomerName());
            vo.setOrderItemId(plan.getOrderItemId());
            OrderItem orderItem = orderItemMap.get(plan.getOrderItemId());
            vo.setProductCode(orderItem == null ? null : orderItem.getProductCode());
            vo.setProductName(orderItem == null ? null : orderItem.getProductName());
            vo.setSpecification(orderItem == null ? null : orderItem.getSpecification());
            vo.setColor(orderItem == null ? null : orderItem.getColor());
            vo.setBatchId(plan.getBatchId());
            BatchInfo batch = batchMap.get(plan.getBatchId());
            vo.setBatchNo(batch == null ? null : batch.getBatchNo());
            vo.setRouteId(plan.getRouteId());
            ProcessRoute route = routeMap.get(plan.getRouteId());
            vo.setRouteName(route == null ? null : route.getRouteName());
            vo.setStatus(plan.getStatus());
            vo.setPlanStartTime(plan.getPlanStartTime());
            vo.setPlanEndTime(plan.getPlanEndTime());
            vo.setRemark(plan.getRemark());
            return vo;
        }).toList();
    }

    private List<OrderPlanStepSummaryVo> buildPlanStepSummary(List<PlanStep> planSteps) {
        if (planSteps.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ProcessStep> stepMap = planSteps.stream()
                .map(PlanStep::getStepId)
                .filter(Objects::nonNull)
                .distinct()
                .map(processStepMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
        Map<Long, Machine> machineMap = planSteps.stream()
                .map(PlanStep::getMachineId)
                .filter(Objects::nonNull)
                .distinct()
                .map(machineMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Machine::getMachineId, Function.identity()));
        return planSteps.stream().map(step -> {
            OrderPlanStepSummaryVo vo = new OrderPlanStepSummaryVo();
            vo.setPlanStepId(step.getPlanStepId());
            vo.setPlanId(step.getPlanId());
            vo.setStepId(step.getStepId());
            ProcessStep processStep = stepMap.get(step.getStepId());
            vo.setStepName(processStep == null ? null : processStep.getStepName());
            vo.setMachineId(step.getMachineId());
            Machine machine = machineMap.get(step.getMachineId());
            vo.setMachineName(machine == null ? null : machine.getMachineName());
            vo.setPlanStartTime(step.getPlanStartTime());
            vo.setPlanEndTime(step.getPlanEndTime());
            vo.setPlanHours(step.getPlanHours());
            vo.setSequenceNo(step.getSequenceNo());
            vo.setStatus(step.getStatus());
            return vo;
        }).toList();
    }

    private List<OrderRouteSummaryVo> buildRouteSummary(List<ProductionPlan> plans) {
        if (plans.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<RouteStep>> routeStepsByRoute = routeStepMapper.selectList(Wrappers.<RouteStep>lambdaQuery()
                        .in(RouteStep::getRouteId, plans.stream().map(ProductionPlan::getRouteId).filter(Objects::nonNull).distinct().toList())
                        .orderByAsc(RouteStep::getRouteId)
                        .orderByAsc(RouteStep::getSortOrder))
                .stream().collect(Collectors.groupingBy(RouteStep::getRouteId));
        Map<Long, ProcessStep> stepMap = routeStepsByRoute.values().stream()
                .flatMap(List::stream)
                .map(RouteStep::getStepId)
                .filter(Objects::nonNull)
                .distinct()
                .map(processStepMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
        return plans.stream()
                .map(ProductionPlan::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .map(processRouteMapper::selectById)
                .filter(Objects::nonNull)
                .map(route -> {
                    OrderRouteSummaryVo vo = new OrderRouteSummaryVo();
                    vo.setRouteId(route.getRouteId());
                    vo.setRouteName(route.getRouteName());
                    vo.setDescription(route.getDescription());
                    vo.setSteps(routeStepsByRoute.getOrDefault(route.getRouteId(), Collections.emptyList())
                            .stream()
                            .map(step -> toRouteStepVo(step, stepMap.get(step.getStepId())))
                            .toList());
                    return vo;
                }).toList();
    }

    private List<OrderMachineSummaryVo> buildMachineSummary(List<PlanStep> planSteps) {
        if (planSteps.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ProcessStep> stepMap = planSteps.stream()
                .map(PlanStep::getStepId)
                .filter(Objects::nonNull)
                .distinct()
                .map(processStepMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
        return planSteps.stream()
                .map(PlanStep::getMachineId)
                .filter(Objects::nonNull)
                .distinct()
                .map(machineMapper::selectById)
                .filter(Objects::nonNull)
                .map(machine -> {
                    List<PlanStep> relatedSteps = planSteps.stream()
                            .filter(step -> Objects.equals(step.getMachineId(), machine.getMachineId()))
                            .toList();
                    OrderMachineSummaryVo vo = new OrderMachineSummaryVo();
                    vo.setMachineId(machine.getMachineId());
                    vo.setMachineCode(machine.getMachineCode());
                    vo.setMachineName(machine.getMachineName());
                    vo.setMachineType(machine.getMachineType());
                    vo.setStatus(machine.getStatus() == null ? null : String.valueOf(machine.getStatus()));
                    vo.setRelatedPlanIds(relatedSteps.stream().map(PlanStep::getPlanId).distinct().toList());
                    vo.setRelatedPlanStepIds(relatedSteps.stream().map(PlanStep::getPlanStepId).toList());
                    vo.setOccupiedTimeRanges(relatedSteps.stream().map(step -> {
                        OrderMachineOccupiedRangeVo range = new OrderMachineOccupiedRangeVo();
                        range.setPlanId(step.getPlanId());
                        range.setPlanStepId(step.getPlanStepId());
                        ProcessStep processStep = stepMap.get(step.getStepId());
                        range.setStepName(processStep == null ? null : processStep.getStepName());
                        range.setPlanStartTime(step.getPlanStartTime());
                        range.setPlanEndTime(step.getPlanEndTime());
                        return range;
                    }).toList());
                    return vo;
                }).toList();
    }

    private List<OrderQcSummaryVo> buildQcSummary(List<PlanStep> planSteps,
                                                  Map<Long, String> stepNameByPlanStepId,
                                                  Map<Long, String> batchNoByPlanId) {
        if (planSteps.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> planIdByPlanStepId = planSteps.stream()
                .collect(Collectors.toMap(PlanStep::getPlanStepId, PlanStep::getPlanId, (left, right) -> left, LinkedHashMap::new));
        List<Long> planStepIds = planSteps.stream().map(PlanStep::getPlanStepId).toList();
        return qcRecordMapper.selectList(Wrappers.<QcRecord>lambdaQuery()
                        .in(QcRecord::getPlanStepId, planStepIds)
                        .orderByDesc(QcRecord::getInspectTime)
                        .last("limit 20"))
                .stream().map(record -> {
                    OrderQcSummaryVo vo = new OrderQcSummaryVo();
                    vo.setInspectionId(record.getInspectionId());
                    vo.setPlanStepId(record.getPlanStepId());
                    vo.setQcItemId(record.getQcItemId());
                    Long planId = planIdByPlanStepId.get(record.getPlanStepId());
                    vo.setPlanId(planId);
                    vo.setStepName(stepNameByPlanStepId.get(record.getPlanStepId()));
                    vo.setBatchNo(planId == null ? null : batchNoByPlanId.get(planId));
                    vo.setResultJudge(record.getResultJudge());
                    vo.setResultValue(record.getResultValue());
                    vo.setInspectType(record.getInspectType());
                    vo.setInspectTime(record.getInspectTime());
                    return vo;
                }).toList();
    }

    private List<OrderExceptionSummaryVo> buildExceptionSummary(List<PlanStep> planSteps,
                                                                Map<Long, String> stepNameByPlanStepId,
                                                                Map<Long, String> batchNoByPlanId) {
        if (planSteps.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> planIdByPlanStepId = planSteps.stream()
                .collect(Collectors.toMap(PlanStep::getPlanStepId, PlanStep::getPlanId, (left, right) -> left, LinkedHashMap::new));
        List<Long> planStepIds = planSteps.stream().map(PlanStep::getPlanStepId).toList();
        return exceptionRecordMapper.selectList(Wrappers.<ExceptionRecord>lambdaQuery()
                        .in(ExceptionRecord::getPlanStepId, planStepIds)
                        .orderByDesc(ExceptionRecord::getCreateTime)
                        .last("limit 20"))
                .stream().map(record -> {
                    OrderExceptionSummaryVo vo = new OrderExceptionSummaryVo();
                    vo.setExceptionId(record.getExceptionId());
                    vo.setPlanStepId(record.getPlanStepId());
                    Long planId = planIdByPlanStepId.get(record.getPlanStepId());
                    vo.setPlanId(planId);
                    vo.setStepName(stepNameByPlanStepId.get(record.getPlanStepId()));
                    vo.setBatchNo(planId == null ? null : batchNoByPlanId.get(planId));
                    vo.setExceptionType(record.getExceptionType());
                    vo.setExceptionLevel(record.getExceptionLevel());
                    vo.setDescription(record.getDescription());
                    vo.setStatus(record.getStatus());
                    vo.setCreateTime(record.getCreateTime());
                    return vo;
                }).toList();
    }

    private long countOrderQcRecords(List<PlanStep> planSteps) {
        if (planSteps.isEmpty()) {
            return 0L;
        }
        return qcRecordMapper.selectCount(Wrappers.<QcRecord>lambdaQuery()
                .in(QcRecord::getPlanStepId, planSteps.stream().map(PlanStep::getPlanStepId).toList()));
    }

    private long countOrderExceptions(List<PlanStep> planSteps) {
        if (planSteps.isEmpty()) {
            return 0L;
        }
        return exceptionRecordMapper.selectCount(Wrappers.<ExceptionRecord>lambdaQuery()
                .in(ExceptionRecord::getPlanStepId, planSteps.stream().map(PlanStep::getPlanStepId).toList()));
    }

    private OrderListVo toListVo(OrderInfo order, Map<Long, Long> batchCountMap, Map<Long, Long> planCountMap) {
        OrderListVo vo = new OrderListVo();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerName(order.getCustomerName());
        vo.setOrderDate(order.getOrderDate());
        vo.setDeliveryDate(order.getDeliveryDate());
        vo.setPriority(order.getPriority());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setLinkedBatchCount(batchCountMap.getOrDefault(order.getOrderId(), 0L));
        vo.setGeneratedPlanCount(planCountMap.getOrDefault(order.getOrderId(), 0L));
        return vo;
    }

    private OrderItemVo toItemVo(OrderItem item) {
        OrderItemVo vo = new OrderItemVo();
        vo.setOrderItemId(item.getOrderItemId());
        vo.setOrderId(item.getOrderId());
        vo.setProductCode(item.getProductCode());
        vo.setProductName(item.getProductName());
        vo.setSpecification(item.getSpecification());
        vo.setColor(item.getColor());
        vo.setQuantity(item.getQuantity());
        vo.setUnit(item.getUnit());
        vo.setRequiredWidth(item.getRequiredWidth());
        vo.setTargetWidth(item.getRequiredWidth());
        vo.setRequiredWeight(item.getRequiredWeight());
        vo.setTargetWeight(item.getRequiredWeight());
        vo.setRemark(item.getRemark());
        return vo;
    }

    private OrderBatchLinkVo toBatchLinkVo(OrderBatchLink link, BatchInfo batch, BatchResourcePoolVo resource) {
        OrderBatchLinkVo vo = new OrderBatchLinkVo();
        vo.setId(link.getId());
        vo.setOrderId(link.getOrderId());
        vo.setOrderItemId(link.getOrderItemId());
        vo.setBatchId(link.getBatchId());
        vo.setBatchNo(batch == null ? null : batch.getBatchNo());
        vo.setSupplier(batch == null ? null : batch.getSupplier());
        vo.setWeight(batch == null ? null : batch.getWeight());
        vo.setWidth(batch == null ? null : batch.getWidth());
        vo.setComposition(batch == null ? null : batch.getComposition());
        vo.setAllocatedWeight(link.getAllocatedWeight());
        vo.setAllocatedQuantity(link.getAllocatedQuantity());
        if (resource != null) {
            vo.setRemainingWeight(resource.getRemainingWeight());
            vo.setRemainingQuantity(resource.getRemainingQuantity());
            vo.setStatus(resource.getStatus());
            vo.setResourceStatus(resource.getResourceStatus());
            vo.setResourceStatusLabel(resource.getResourceStatusLabel());
            vo.setCurrentPlanId(resource.getCurrentPlanId());
            vo.setCurrentPlanStatus(resource.getCurrentPlanStatus());
            vo.setLockedByPlan(resource.isLockedByPlan());
            vo.setReadyForSchedule(resource.isReadyForSchedule());
        }
        vo.setRemark(link.getRemark());
        return vo;
    }

    private OrderSchedulePoolVo toSchedulePoolVo(OrderInfo order,
                                                 OrderItem item,
                                                 List<OrderBatchLink> links,
                                                 Map<Long, BatchInfo> batchMap,
                                                 Map<Long, BatchResourcePoolVo> batchResourceMap,
                                                 List<ProductionPlan> plans,
                                                 List<ProcessRoute> activeRoutes,
                                                 Map<Long, List<RouteStep>> routeStepsByRoute,
                                                 Map<Long, ProcessStep> routeProcessStepMap,
                                                 Map<Long, List<StepMachineCapability>> capabilitiesByStep,
                                                 Map<Long, Machine> machineMap) {
        List<OrderBatchLinkVo> linkedBatches = links.stream()
                .map(link -> toBatchLinkVo(link, batchMap.get(link.getBatchId()), batchResourceMap.get(link.getBatchId())))
                .toList();
        long activePlanCount = plans.stream().filter(this::isActivePlan).count();
        long readyBatchCount = countReadyBatches(links, batchResourceMap);
        RouteRecommendation recommendation = recommendRoute(
                item,
                links,
                batchMap,
                activeRoutes,
                routeStepsByRoute,
                routeProcessStepMap,
                capabilitiesByStep,
                machineMap
        );
        String scheduleBlockedReason = resolveScheduleBlockedReason(order, links, readyBatchCount, recommendation, activePlanCount);
        OrderSchedulePoolVo vo = new OrderSchedulePoolVo();
        vo.setOrderId(order == null ? null : order.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setCustomerName(order == null ? null : order.getCustomerName());
        vo.setOrderDate(order == null ? null : order.getOrderDate());
        vo.setOrderItemId(item.getOrderItemId());
        vo.setProductCode(item.getProductCode());
        vo.setProductName(item.getProductName());
        vo.setSpecification(item.getSpecification());
        vo.setColor(item.getColor());
        vo.setQuantity(item.getQuantity());
        vo.setUnit(item.getUnit());
        vo.setRequiredWidth(item.getRequiredWidth());
        vo.setRequiredWeight(item.getRequiredWeight());
        vo.setDeliveryDate(order == null ? null : order.getDeliveryDate());
        vo.setPriority(order == null ? null : order.getPriority());
        vo.setRemark(order == null ? null : order.getRemark());
        vo.setLinkedBatchCount(links.stream().map(OrderBatchLink::getBatchId).distinct().count());
        vo.setReadyBatchCount(readyBatchCount);
        vo.setGeneratedPlanCount(plans.size());
        vo.setActivePlanCount(activePlanCount);
        vo.setBatchSummary(buildBatchSummary(links, batchMap));
        vo.setAllocatedWeight(sumBigDecimal(links.stream().map(OrderBatchLink::getAllocatedWeight).toList()));
        vo.setAllocatedQuantity(sumBigDecimal(links.stream().map(OrderBatchLink::getAllocatedQuantity).toList()));
        vo.setLinkedBatches(linkedBatches);
        vo.setRecommendedRouteId(recommendation.routeId());
        vo.setRecommendedRouteName(recommendation.routeName());
        vo.setRecommendedRoute(recommendation.routeSummary());
        vo.setRecommendedMachines(recommendation.machineNames());
        vo.setRecommendedMachineList(recommendation.machines());
        vo.setStatus(order == null ? null : order.getStatus());
        vo.setScheduleBlockedReason(scheduleBlockedReason);
        vo.setReadyForSchedule(scheduleBlockedReason == null);
        return vo;
    }

    private String buildBatchSummary(List<OrderBatchLink> links, Map<Long, BatchInfo> batchMap) {
        if (links.isEmpty()) {
            return "";
        }
        return links.stream()
                .map(link -> {
                    BatchInfo batch = batchMap.get(link.getBatchId());
                    String batchNo = batch == null ? String.valueOf(link.getBatchId()) : batch.getBatchNo();
                    List<String> parts = new ArrayList<>();
                    parts.add(batchNo);
                    if (link.getAllocatedWeight() != null) {
                        parts.add("weight=" + link.getAllocatedWeight().stripTrailingZeros().toPlainString());
                    }
                    if (link.getAllocatedQuantity() != null) {
                        parts.add("qty=" + link.getAllocatedQuantity().stripTrailingZeros().toPlainString());
                    }
                    return String.join(" ", parts);
                })
                .collect(Collectors.joining("; "));
    }

    private long countReadyBatches(List<OrderBatchLink> links, Map<Long, BatchResourcePoolVo> batchResourceMap) {
        return links.stream()
                .map(OrderBatchLink::getBatchId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(batchId -> {
                    BatchResourcePoolVo resource = batchResourceMap.get(batchId);
                    return resource != null && resource.isReadyForSchedule();
                })
                .count();
    }

    private String resolveScheduleBlockedReason(OrderInfo order,
                                                List<OrderBatchLink> links,
                                                long readyBatchCount,
                                                RouteRecommendation recommendation,
                                                long activePlanCount) {
        if (order == null) {
            return "Order context is missing";
        }
        if ("DONE".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            return "The order status does not allow scheduling";
        }
        if (links.isEmpty()) {
            return "No batch is linked to the order item";
        }
        if (activePlanCount > 0) {
            return "The order item already has an active production plan";
        }
        if (readyBatchCount <= 0) {
            return "No linked batch is currently ready for scheduling";
        }
        if (recommendation.routeSummary() == null) {
            return "No active process route is available";
        }
        if (recommendation.machines().isEmpty()) {
            return "No active machine recommendation is available for the linked batches";
        }
        return null;
    }

    private RouteRecommendation recommendRoute(OrderItem item,
                                               List<OrderBatchLink> links,
                                               Map<Long, BatchInfo> batchMap,
                                               List<ProcessRoute> activeRoutes,
                                               Map<Long, List<RouteStep>> routeStepsByRoute,
                                               Map<Long, ProcessStep> routeProcessStepMap,
                                               Map<Long, List<StepMachineCapability>> capabilitiesByStep,
                                               Map<Long, Machine> machineMap) {
        if (links.isEmpty() || activeRoutes.isEmpty()) {
            return RouteRecommendation.empty();
        }
        List<BatchInfo> batches = links.stream()
                .map(link -> batchMap.get(link.getBatchId()))
                .filter(Objects::nonNull)
                .toList();
        if (batches.isEmpty()) {
            return RouteRecommendation.empty();
        }

        ProcessRoute preferred = activeRoutes.stream()
                .filter(route -> routeMatches(route, item, batches))
                .findFirst()
                .orElse(activeRoutes.get(0));

        List<RouteStep> routeSteps = routeStepsByRoute.getOrDefault(preferred.getRouteId(), Collections.emptyList());
        LinkedHashMap<Long, OrderSchedulePoolMachineVo> machineMapById = new LinkedHashMap<>();
        for (RouteStep routeStep : routeSteps) {
            List<StepMachineCapability> capabilities = capabilitiesByStep.getOrDefault(routeStep.getStepId(), Collections.emptyList());
            capabilities.stream()
                    .filter(capability -> supportsAnyBatch(capability, batches))
                    .map(capability -> machineMap.get(capability.getMachineId()))
                    .filter(Objects::nonNull)
                    .limit(2)
                    .forEach(machine -> machineMapById.putIfAbsent(machine.getMachineId(), toSchedulePoolMachineVo(machine)));
            if (machineMapById.size() >= 6) {
                break;
            }
        }

        return new RouteRecommendation(
                toRouteSummaryVo(preferred, routeSteps, routeProcessStepMap),
                List.copyOf(machineMapById.values())
        );
    }

    private OrderRouteSummaryVo toRouteSummaryVo(ProcessRoute route,
                                                 List<RouteStep> routeSteps,
                                                 Map<Long, ProcessStep> routeProcessStepMap) {
        OrderRouteSummaryVo vo = new OrderRouteSummaryVo();
        vo.setRouteId(route.getRouteId());
        vo.setRouteName(route.getRouteName());
        vo.setDescription(route.getDescription());
        vo.setSteps(routeSteps.stream()
                .map(step -> toRouteStepVo(step, routeProcessStepMap.get(step.getStepId())))
                .toList());
        return vo;
    }

    private OrderSchedulePoolMachineVo toSchedulePoolMachineVo(Machine machine) {
        OrderSchedulePoolMachineVo vo = new OrderSchedulePoolMachineVo();
        vo.setMachineId(machine.getMachineId());
        vo.setMachineCode(machine.getMachineCode());
        vo.setMachineName(machine.getMachineName());
        vo.setMachineType(machine.getMachineType());
        vo.setStatus(machine.getStatus() == null ? null : String.valueOf(machine.getStatus()));
        return vo;
    }

    private BigDecimal sumBigDecimal(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isActivePlan(ProductionPlan plan) {
        return plan != null && ACTIVE_PLAN_STATUSES.contains(plan.getStatus());
    }

    private OrderRouteStepVo toRouteStepVo(RouteStep routeStep, ProcessStep processStep) {
        OrderRouteStepVo vo = new OrderRouteStepVo();
        vo.setRouteStepId(routeStep.getRouteStepId());
        vo.setStepId(routeStep.getStepId());
        vo.setSortOrder(routeStep.getSortOrder());
        vo.setIsMandatory(routeStep.getIsMandatory());
        if (processStep != null) {
            vo.setStepCode(processStep.getStepCode());
            vo.setStepName(processStep.getStepName());
        }
        return vo;
    }

    private void copyOrderRequest(OrderUpsertRequest request, OrderInfo entity) {
        entity.setOrderNo(request.getOrderNo().trim());
        entity.setCustomerName(request.getCustomerName().trim());
        entity.setOrderDate(request.getOrderDate());
        entity.setDeliveryDate(request.getDeliveryDate());
        entity.setPriority(request.getPriority());
        entity.setStatus(request.getStatus().trim());
        entity.setRemark(request.getRemark());
    }

    private void copyOrderItemRequest(Long orderId, OrderItemUpsertRequest request, OrderItem entity) {
        entity.setOrderId(orderId);
        entity.setProductCode(request.getProductCode().trim());
        entity.setProductName(request.getProductName().trim());
        entity.setSpecification(request.getSpecification());
        entity.setColor(request.getColor());
        entity.setQuantity(request.getQuantity());
        entity.setUnit(request.getUnit());
        entity.setRequiredWidth(request.getRequiredWidth());
        entity.setRequiredWeight(request.getRequiredWeight());
        entity.setRemark(request.getRemark());
    }

    private void copyBatchLinkRequest(Long orderId, OrderBatchLinkUpsertRequest request, OrderBatchLink entity) {
        entity.setOrderId(orderId);
        entity.setOrderItemId(request.getOrderItemId());
        entity.setBatchId(request.getBatchId());
        entity.setAllocatedWeight(request.getAllocatedWeight());
        entity.setAllocatedQuantity(request.getAllocatedQuantity());
        entity.setRemark(request.getRemark());
    }

    private void validateOrderDates(LocalDateTime orderDate, LocalDateTime deliveryDate) {
        if (orderDate != null && deliveryDate != null && orderDate.isAfter(deliveryDate)) {
            throw new BusinessException(422, "orderDate must be earlier than or equal to deliveryDate");
        }
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }

    private void validateOrderItemRequest(OrderItemUpsertRequest request) {
        if (request.getRequiredWidth() != null && request.getRequiredWidth().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(422, "requiredWidth must be greater than or equal to 0");
        }
        if (request.getRequiredWeight() != null && request.getRequiredWeight().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(422, "requiredWeight must be greater than or equal to 0");
        }
    }

    private void validateOrderBatchLinkRequest(OrderBatchLinkUpsertRequest request, OrderItem item, BatchInfo batch) {
        if (request.getAllocatedWeight() == null && request.getAllocatedQuantity() == null) {
            throw new BusinessException(422, "allocatedWeight or allocatedQuantity must provide at least one allocation dimension");
        }
        if (request.getAllocatedWeight() != null && batch.getWeight() != null && request.getAllocatedWeight().compareTo(batch.getWeight()) > 0) {
            throw new BusinessException(422, "allocatedWeight exceeds batch weight");
        }
        if (request.getAllocatedWeight() != null && item.getRequiredWeight() != null && request.getAllocatedWeight().compareTo(item.getRequiredWeight()) > 0) {
            throw new BusinessException(422, "allocatedWeight exceeds order item requiredWeight");
        }
        if (request.getAllocatedWeight() != null && batch.getWeight() != null) {
            BigDecimal batchAllocatedWeight = batchResourcePoolService.sumBatchAllocatedWeight(batch.getBatchId(), null)
                    .add(request.getAllocatedWeight());
            if (batchAllocatedWeight.compareTo(batch.getWeight()) > 0) {
                throw new BusinessException(422, "allocatedWeight exceeds remaining batch weight");
            }
        }
        if (request.getAllocatedWeight() != null && item.getRequiredWeight() != null) {
            BigDecimal itemAllocatedWeight = batchResourcePoolService.sumOrderItemAllocatedWeight(item.getOrderItemId(), null)
                    .add(request.getAllocatedWeight());
            if (itemAllocatedWeight.compareTo(item.getRequiredWeight()) > 0) {
                throw new BusinessException(422, "allocatedWeight exceeds remaining order item requiredWeight");
            }
        }
        if (request.getAllocatedQuantity() != null && item.getQuantity() != null) {
            BigDecimal itemAllocatedQuantity = batchResourcePoolService.sumOrderItemAllocatedQuantity(item.getOrderItemId(), null)
                    .add(request.getAllocatedQuantity());
            if (itemAllocatedQuantity.compareTo(item.getQuantity()) > 0) {
                throw new BusinessException(422, "allocatedQuantity exceeds remaining order item quantity");
            }
        }
        if (item.getRequiredWidth() != null && batch.getWidth() != null && batch.getWidth().compareTo(item.getRequiredWidth()) < 0) {
            throw new BusinessException(422, "batch width does not satisfy order item requiredWidth");
        }
    }

    private void checkOrderNoUnique(String orderNo, Long excludeId) {
        long count = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getOrderNo, orderNo)
                .ne(excludeId != null, OrderInfo::getOrderId, excludeId));
        if (count > 0) {
            throw new BusinessException(409, "Order number already exists");
        }
    }

    private void checkBatchLinkUnique(Long orderId, Long orderItemId, Long batchId, Long excludeId) {
        long count = orderBatchLinkMapper.selectCount(Wrappers.<OrderBatchLink>lambdaQuery()
                .eq(OrderBatchLink::getOrderId, orderId)
                .eq(OrderBatchLink::getOrderItemId, orderItemId)
                .eq(OrderBatchLink::getBatchId, batchId)
                .ne(excludeId != null, OrderBatchLink::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(409, "The order item is already linked to the batch");
        }
    }

    private void assertOrderStatus(String status) {
        if (!ORDER_STATUSES.contains(status)) {
            throw new BusinessException(422, "Unsupported order status: " + status);
        }
    }

    private OrderBatchLink requireBatchLink(Long linkId) {
        OrderBatchLink link = orderBatchLinkMapper.selectById(linkId);
        if (link == null) {
            throw new BusinessException(404, "Order batch link not found");
        }
        return link;
    }

    private boolean routeMatches(ProcessRoute route, OrderItem item, List<BatchInfo> batches) {
        String routeName = route.getRouteName() == null ? "" : route.getRouteName();
        String itemName = item.getProductName() == null ? "" : item.getProductName();
        String specification = item.getSpecification() == null ? "" : item.getSpecification();
        String composition = batches.stream()
                .map(BatchInfo::getComposition)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));

        String profile = (itemName + " " + specification + " " + composition).toLowerCase();
        if (profile.contains("涤氨") || (profile.contains("涤纶") && profile.contains("氨纶"))) {
            return routeName.contains("涤氨");
        }
        if (profile.contains("工装") || profile.contains("功能")) {
            return routeName.contains("功能");
        }
        if (profile.contains("婴童") || profile.contains("棉") || profile.contains("莫代尔")) {
            return routeName.contains("棉");
        }
        return false;
    }

    private boolean supportsAnyBatch(StepMachineCapability capability, List<BatchInfo> batches) {
        return batches.stream().anyMatch(batch -> {
            boolean widthMatched = batch.getWidth() == null
                    || ((capability.getMinWidth() == null || capability.getMinWidth().compareTo(batch.getWidth()) <= 0)
                    && (capability.getMaxWidth() == null || capability.getMaxWidth().compareTo(batch.getWidth()) >= 0));
            boolean weightMatched = batch.getWeight() == null
                    || capability.getMaxBatchWeight() == null
                    || capability.getMaxBatchWeight().compareTo(batch.getWeight()) >= 0;
            return widthMatched && weightMatched;
        });
    }

    private record RouteRecommendation(OrderRouteSummaryVo routeSummary, List<OrderSchedulePoolMachineVo> machines) {
        private Long routeId() {
            return routeSummary == null ? null : routeSummary.getRouteId();
        }

        private String routeName() {
            return routeSummary == null ? null : routeSummary.getRouteName();
        }

        private List<String> machineNames() {
            return machines.stream()
                    .map(OrderSchedulePoolMachineVo::getMachineName)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        private static RouteRecommendation empty() {
            return new RouteRecommendation(null, Collections.emptyList());
        }
    }
}
