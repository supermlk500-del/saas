package com.zhihuitong.modules.batch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.dto.BatchQuery;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.enums.BatchResourceStatusEnum;
import com.zhihuitong.modules.batch.mapper.BatchInfoMapper;
import com.zhihuitong.modules.batch.vo.BatchLinkedOrderVo;
import com.zhihuitong.modules.batch.vo.BatchResourcePoolVo;
import com.zhihuitong.modules.order.entity.OrderBatchLink;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.mapper.OrderBatchLinkMapper;
import com.zhihuitong.modules.order.mapper.OrderInfoMapper;
import com.zhihuitong.modules.order.mapper.OrderItemMapper;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BatchResourcePoolService {

    private static final Set<String> ACTIVE_PLAN_STATUSES = Set.of("DRAFT", "RELEASED", "RUNNING");

    private final BatchInfoMapper batchInfoMapper;
    private final OrderBatchLinkMapper orderBatchLinkMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductionPlanMapper productionPlanMapper;

    public BatchResourcePoolService(BatchInfoMapper batchInfoMapper,
                                    OrderBatchLinkMapper orderBatchLinkMapper,
                                    OrderInfoMapper orderInfoMapper,
                                    OrderItemMapper orderItemMapper,
                                    ProductionPlanMapper productionPlanMapper) {
        this.batchInfoMapper = batchInfoMapper;
        this.orderBatchLinkMapper = orderBatchLinkMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productionPlanMapper = productionPlanMapper;
    }

    public TableDataInfo<BatchResourcePoolVo> list(BatchQuery query) {
        validateDateRange(query.getDateFrom(), query.getDateTo(), "dateFrom must be earlier than or equal to dateTo");
        Page<BatchInfo> page = batchInfoMapper.selectPage(query.toPage(), buildQuery(query));
        List<BatchResourcePoolVo> rows = buildResources(page.getRecords());
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public BatchResourcePoolVo getResource(Long batchId) {
        BatchInfo batchInfo = batchInfoMapper.selectById(batchId);
        if (batchInfo == null) {
            throw new BusinessException(404, "Batch not found");
        }
        return buildResources(List.of(batchInfo)).get(0);
    }

    public Map<Long, BatchResourcePoolVo> buildResourceMap(Collection<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> uniqueBatchIds = batchIds.stream().filter(Objects::nonNull).distinct().toList();
        if (uniqueBatchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BatchInfo> batches = batchInfoMapper.selectList(Wrappers.<BatchInfo>lambdaQuery()
                .in(BatchInfo::getBatchId, uniqueBatchIds));
        return buildResources(batches).stream()
                .collect(Collectors.toMap(BatchResourcePoolVo::getBatchId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    public BigDecimal sumBatchAllocatedWeight(Long batchId, Long excludeLinkId) {
        return orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                        .eq(OrderBatchLink::getBatchId, batchId)
                        .ne(excludeLinkId != null, OrderBatchLink::getId, excludeLinkId))
                .stream()
                .map(OrderBatchLink::getAllocatedWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumOrderItemAllocatedWeight(Long orderItemId, Long excludeLinkId) {
        return orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                        .eq(OrderBatchLink::getOrderItemId, orderItemId)
                        .ne(excludeLinkId != null, OrderBatchLink::getId, excludeLinkId))
                .stream()
                .map(OrderBatchLink::getAllocatedWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumOrderItemAllocatedQuantity(Long orderItemId, Long excludeLinkId) {
        return orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                        .eq(OrderBatchLink::getOrderItemId, orderItemId)
                        .ne(excludeLinkId != null, OrderBatchLink::getId, excludeLinkId))
                .stream()
                .map(OrderBatchLink::getAllocatedQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<BatchResourcePoolVo> buildResources(List<BatchInfo> batches) {
        if (batches.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> batchIds = batches.stream().map(BatchInfo::getBatchId).toList();
        List<OrderBatchLink> links = orderBatchLinkMapper.selectList(Wrappers.<OrderBatchLink>lambdaQuery()
                .in(OrderBatchLink::getBatchId, batchIds)
                .orderByAsc(OrderBatchLink::getOrderId)
                .orderByAsc(OrderBatchLink::getOrderItemId));
        List<ProductionPlan> plans = productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .in(ProductionPlan::getBatchId, batchIds)
                .orderByDesc(ProductionPlan::getCreateTime));

        Map<Long, List<OrderBatchLink>> linksByBatch = links.stream()
                .collect(Collectors.groupingBy(OrderBatchLink::getBatchId));
        Map<Long, List<ProductionPlan>> plansByBatch = plans.stream()
                .collect(Collectors.groupingBy(ProductionPlan::getBatchId));
        Map<Long, OrderInfo> orderMap = fetchOrderMap(links);
        Map<Long, OrderItem> itemMap = fetchItemMap(links);

        return batches.stream()
                .map(batch -> toResourceVo(
                        batch,
                        linksByBatch.getOrDefault(batch.getBatchId(), Collections.emptyList()),
                        plansByBatch.getOrDefault(batch.getBatchId(), Collections.emptyList()),
                        orderMap,
                        itemMap
                ))
                .toList();
    }

    private Map<Long, OrderInfo> fetchOrderMap(List<OrderBatchLink> links) {
        List<Long> orderIds = links.stream().map(OrderBatchLink::getOrderId).filter(Objects::nonNull).distinct().toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery().in(OrderInfo::getOrderId, orderIds))
                .stream()
                .collect(Collectors.toMap(OrderInfo::getOrderId, Function.identity()));
    }

    private Map<Long, OrderItem> fetchItemMap(List<OrderBatchLink> links) {
        List<Long> itemIds = links.stream().map(OrderBatchLink::getOrderItemId).filter(Objects::nonNull).distinct().toList();
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery().in(OrderItem::getOrderItemId, itemIds))
                .stream()
                .collect(Collectors.toMap(OrderItem::getOrderItemId, Function.identity()));
    }

    private BatchResourcePoolVo toResourceVo(BatchInfo batch,
                                             List<OrderBatchLink> links,
                                             List<ProductionPlan> plans,
                                             Map<Long, OrderInfo> orderMap,
                                             Map<Long, OrderItem> itemMap) {
        BigDecimal allocatedWeight = sum(links.stream().map(OrderBatchLink::getAllocatedWeight).toList());
        BigDecimal allocatedQuantity = sum(links.stream().map(OrderBatchLink::getAllocatedQuantity).toList());
        // `batchinfo` has no native quantity column, so linked order-item quantity is the safest compatible baseline.
        BigDecimal linkedQuantityTotal = sum(links.stream()
                .map(OrderBatchLink::getOrderItemId)
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .map(OrderItem::getQuantity)
                .toList());
        BigDecimal remainingWeight = subtractNonNegative(batch.getWeight(), allocatedWeight);
        BigDecimal remainingQuantity = links.isEmpty() ? null : subtractNonNegative(linkedQuantityTotal, allocatedQuantity);
        ProductionPlan currentPlan = findCurrentPlan(plans);
        BatchResourceStatusEnum resourceStatus = deriveResourceStatus(
                links,
                plans,
                batch.getWeight(),
                allocatedWeight,
                remainingWeight,
                linkedQuantityTotal,
                allocatedQuantity,
                remainingQuantity
        );

        BatchResourcePoolVo vo = new BatchResourcePoolVo();
        vo.setBatchId(batch.getBatchId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setSupplier(batch.getSupplier());
        vo.setInDate(batch.getInDate());
        vo.setWeight(batch.getWeight());
        vo.setWidth(batch.getWidth());
        vo.setComposition(batch.getComposition());
        vo.setNote(batch.getNote());
        vo.setStatus(deriveLegacyStatus(plans));
        vo.setResourceStatus(resourceStatus.getCode());
        vo.setResourceStatusLabel(resourceStatus.getLabel());
        vo.setLinkedOrderCount(links.stream().map(OrderBatchLink::getOrderId).filter(Objects::nonNull).distinct().count());
        vo.setLinkedOrders(links.stream().map(link -> toLinkedOrderVo(link, orderMap, itemMap)).toList());
        vo.setAllocatedWeight(allocatedWeight);
        vo.setAllocatedQuantity(allocatedQuantity);
        vo.setRemainingWeight(remainingWeight);
        vo.setRemainingQuantity(remainingQuantity);
        vo.setCurrentPlanId(currentPlan == null ? null : currentPlan.getPlanId());
        vo.setCurrentPlanStatus(currentPlan == null ? null : currentPlan.getStatus());
        vo.setLockedByPlan(plans.stream().anyMatch(this::isActivePlan));
        fillCurrentOrder(vo, currentPlan, links, orderMap);
        boolean hasExecutableAllocation = hasPositiveAllocation(allocatedWeight) || hasPositiveAllocation(allocatedQuantity);
        vo.setReadyForSchedule(!vo.isLockedByPlan()
                && hasExecutableAllocation
                && vo.getLinkedOrderCount() > 0
                && resourceStatus != BatchResourceStatusEnum.CONSUMED
                && resourceStatus != BatchResourceStatusEnum.CLOSED);
        return vo;
    }

    private BatchLinkedOrderVo toLinkedOrderVo(OrderBatchLink link,
                                               Map<Long, OrderInfo> orderMap,
                                               Map<Long, OrderItem> itemMap) {
        OrderInfo order = orderMap.get(link.getOrderId());
        OrderItem item = itemMap.get(link.getOrderItemId());
        BatchLinkedOrderVo vo = new BatchLinkedOrderVo();
        vo.setLinkId(link.getId());
        vo.setOrderId(link.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setCustomerName(order == null ? null : order.getCustomerName());
        vo.setOrderStatus(order == null ? null : order.getStatus());
        vo.setOrderItemId(link.getOrderItemId());
        vo.setProductCode(item == null ? null : item.getProductCode());
        vo.setProductName(item == null ? null : item.getProductName());
        vo.setSpecification(item == null ? null : item.getSpecification());
        vo.setAllocatedWeight(link.getAllocatedWeight());
        vo.setAllocatedQuantity(link.getAllocatedQuantity());
        vo.setRemark(link.getRemark());
        return vo;
    }

    private void fillCurrentOrder(BatchResourcePoolVo vo,
                                  ProductionPlan currentPlan,
                                  List<OrderBatchLink> links,
                                  Map<Long, OrderInfo> orderMap) {
        Long currentOrderId = currentPlan == null ? null : currentPlan.getOrderId();
        if (currentOrderId == null && !links.isEmpty()) {
            currentOrderId = links.get(0).getOrderId();
        }
        OrderInfo order = currentOrderId == null ? null : orderMap.get(currentOrderId);
        vo.setCurrentOrderId(currentOrderId);
        vo.setCurrentOrderNo(order == null ? null : order.getOrderNo());
    }

    private ProductionPlan findCurrentPlan(List<ProductionPlan> plans) {
        if (plans.isEmpty()) {
            return null;
        }
        return plans.stream()
                .sorted(Comparator.comparing(ProductionPlan::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(this::isActivePlan)
                .findFirst()
                .orElse(plans.get(0));
    }

    private boolean isActivePlan(ProductionPlan plan) {
        return plan != null && ACTIVE_PLAN_STATUSES.contains(plan.getStatus());
    }

    private BatchResourceStatusEnum deriveResourceStatus(List<OrderBatchLink> links,
                                                         List<ProductionPlan> plans,
                                                         BigDecimal batchWeight,
                                                         BigDecimal allocatedWeight,
                                                         BigDecimal remainingWeight,
                                                         BigDecimal linkedQuantityTotal,
                                                         BigDecimal allocatedQuantity,
                                                         BigDecimal remainingQuantity) {
        if (!plans.isEmpty() && plans.stream().allMatch(plan -> "CANCELLED".equals(plan.getStatus()))) {
            return BatchResourceStatusEnum.CLOSED;
        }
        if (plans.stream().anyMatch(plan -> "RUNNING".equals(plan.getStatus()))) {
            return BatchResourceStatusEnum.IN_EXECUTION;
        }
        if (!plans.isEmpty() && plans.stream().allMatch(plan -> "COMPLETED".equals(plan.getStatus()))) {
            return BatchResourceStatusEnum.CONSUMED;
        }
        if (links.isEmpty()) {
            return BatchResourceStatusEnum.UNALLOCATED;
        }
        if (!hasPositiveAllocation(allocatedWeight) && !hasPositiveAllocation(allocatedQuantity)) {
            return BatchResourceStatusEnum.UNALLOCATED;
        }
        boolean weightTracked = batchWeight != null;
        boolean quantityTracked = linkedQuantityTotal != null;
        boolean hasRemaining = (weightTracked && remainingWeight != null && remainingWeight.compareTo(BigDecimal.ZERO) > 0)
                || (quantityTracked && remainingQuantity != null && remainingQuantity.compareTo(BigDecimal.ZERO) > 0);
        if (hasRemaining) {
            return BatchResourceStatusEnum.PARTIALLY_ALLOCATED;
        }
        return BatchResourceStatusEnum.ALLOCATED;
    }

    private String deriveLegacyStatus(List<ProductionPlan> plans) {
        if (plans.isEmpty()) {
            return "READY";
        }
        Set<String> statuses = plans.stream()
                .map(ProductionPlan::getStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (statuses.contains("RUNNING")) {
            return "IN_PROGRESS";
        }
        if (statuses.contains("RELEASED") || statuses.contains("DRAFT")) {
            return "PLANNED";
        }
        if (statuses.size() == 1 && statuses.contains("COMPLETED")) {
            return "DONE";
        }
        if (statuses.size() == 1 && statuses.contains("CANCELLED")) {
            return "CANCELLED";
        }
        return "PLANNED";
    }

    private LambdaQueryWrapper<BatchInfo> buildQuery(BatchQuery query) {
        return Wrappers.<BatchInfo>lambdaQuery()
                .like(StringUtils.hasText(query.getBatchNo()), BatchInfo::getBatchNo, query.getBatchNo())
                .like(StringUtils.hasText(query.getSupplier()), BatchInfo::getSupplier, query.getSupplier())
                .ge(query.getDateFrom() != null, BatchInfo::getInDate, query.getDateFrom())
                .le(query.getDateTo() != null, BatchInfo::getInDate, query.getDateTo())
                .orderByDesc(BatchInfo::getInDate);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasPositiveAllocation(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal subtractNonNegative(BigDecimal total, BigDecimal used) {
        if (total == null) {
            return null;
        }
        BigDecimal remaining = total.subtract(used == null ? BigDecimal.ZERO : used);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }
}
