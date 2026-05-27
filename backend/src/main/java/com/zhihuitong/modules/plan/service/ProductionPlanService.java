package com.zhihuitong.modules.plan.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.plan.algorithm.ScheduleMachineAssignment;
import com.zhihuitong.modules.plan.algorithm.SchedulingAlgorithmService;
import com.zhihuitong.modules.plan.dto.PlanRescheduleRequest;
import com.zhihuitong.modules.plan.dto.ProductionPlanCreateRequest;
import com.zhihuitong.modules.plan.dto.ProductionPlanQuery;
import com.zhihuitong.modules.plan.dto.ProductionPlanUpdateRequest;
import com.zhihuitong.modules.plan.dto.StatusPatchRequest;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.plan.vo.GanttTaskVo;
import com.zhihuitong.modules.plan.vo.PlanStepVo;
import com.zhihuitong.modules.plan.vo.ProductionPlanDetailVo;
import com.zhihuitong.modules.plan.vo.ProductionPlanListVo;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.entity.ProcessRoute;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.entity.RouteStep;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.service.MachineService;
import com.zhihuitong.modules.process.service.ProcessRouteService;
import com.zhihuitong.modules.process.service.ProcessStepService;
import com.zhihuitong.modules.process.service.StepMachineCapabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductionPlanService {

    private static final Set<String> PLAN_STATUSES = Set.of("DRAFT", "RELEASED", "RUNNING", "COMPLETED", "CANCELLED");
    private static final Set<String> ACTIVE_PLAN_STATUSES = Set.of("DRAFT", "RELEASED", "RUNNING");

    private final ProductionPlanMapper productionPlanMapper;
    private final PlanStepMapper planStepMapper;
    private final BatchService batchService;
    private final OrderService orderService;
    private final ProcessRouteService processRouteService;
    private final ProcessStepService processStepService;
    private final MachineService machineService;
    private final StepMachineCapabilityService capabilityService;
    private final SchedulingAlgorithmService schedulingAlgorithmService;

    public ProductionPlanService(ProductionPlanMapper productionPlanMapper,
                                 PlanStepMapper planStepMapper,
                                 BatchService batchService,
                                 OrderService orderService,
                                 ProcessRouteService processRouteService,
                                 ProcessStepService processStepService,
                                 MachineService machineService,
                                 StepMachineCapabilityService capabilityService,
                                 SchedulingAlgorithmService schedulingAlgorithmService) {
        this.productionPlanMapper = productionPlanMapper;
        this.planStepMapper = planStepMapper;
        this.batchService = batchService;
        this.orderService = orderService;
        this.processRouteService = processRouteService;
        this.processStepService = processStepService;
        this.machineService = machineService;
        this.capabilityService = capabilityService;
        this.schedulingAlgorithmService = schedulingAlgorithmService;
    }

    public TableDataInfo<ProductionPlanListVo> list(ProductionPlanQuery query) {
        validateTimeRange(query.getPlanStartFrom(), query.getPlanStartTo(), "planStartFrom 不能晚于 planStartTo");
        Page<ProductionPlan> page = productionPlanMapper.selectPage(query.toPage(), Wrappers.<ProductionPlan>lambdaQuery()
                .eq(query.getOrderId() != null, ProductionPlan::getOrderId, query.getOrderId())
                .eq(query.getOrderItemId() != null, ProductionPlan::getOrderItemId, query.getOrderItemId())
                .eq(query.getBatchId() != null, ProductionPlan::getBatchId, query.getBatchId())
                .eq(query.getRouteId() != null, ProductionPlan::getRouteId, query.getRouteId())
                .eq(StringUtils.hasText(query.getStatus()), ProductionPlan::getStatus, query.getStatus())
                .ge(query.getPlanStartFrom() != null, ProductionPlan::getPlanStartTime, query.getPlanStartFrom())
                .le(query.getPlanStartTo() != null, ProductionPlan::getPlanStartTime, query.getPlanStartTo())
                .orderByDesc(ProductionPlan::getCreateTime));
        List<ProductionPlan> records = page.getRecords();
        if (records.isEmpty()) {
            return TableDataInfoBuilder.build(Collections.emptyList(), page.getTotal());
        }
        Map<Long, OrderInfo> orderMap = orderService.fetchOrderMap(records.stream().map(ProductionPlan::getOrderId).toList());
        Map<Long, OrderItem> orderItemMap = orderService.fetchOrderItemMap(records.stream().map(ProductionPlan::getOrderItemId).toList());
        Map<Long, BatchInfo> batchMap = fetchBatchMap(records.stream().map(ProductionPlan::getBatchId).toList());
        Map<Long, ProcessRoute> routeMap = fetchRouteMap(records.stream().map(ProductionPlan::getRouteId).toList());
        List<ProductionPlanListVo> rows = records.stream().map(item -> toListVo(
                item,
                orderMap.get(item.getOrderId()),
                orderItemMap.get(item.getOrderItemId()),
                batchMap.get(item.getBatchId()),
                routeMap.get(item.getRouteId())
        )).toList();
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public ProductionPlanDetailVo getDetail(Long planId) {
        ProductionPlan plan = requirePlan(planId);
        ProductionPlanDetailVo detail = new ProductionPlanDetailVo();
        detail.setPlanId(plan.getPlanId());
        detail.setOrderInfo(plan.getOrderId() == null ? null : orderService.requireOrder(plan.getOrderId()));
        detail.setOrderItemInfo(plan.getOrderItemId() == null ? null : orderService.requireOrderItem(plan.getOrderItemId()));
        detail.setBatchInfo(batchService.requireBatch(plan.getBatchId()));
        detail.setRouteInfo(processRouteService.requireRoute(plan.getRouteId()));
        detail.setPlanStartTime(plan.getPlanStartTime());
        detail.setPlanEndTime(plan.getPlanEndTime());
        detail.setStatus(plan.getStatus());
        detail.setRemark(plan.getRemark());
        detail.setPlanSteps(buildPlanStepVos(findPlanSteps(planId)));
        return detail;
    }

    @Transactional
    public Map<String, Object> create(ProductionPlanCreateRequest request) {
        validateTimeRange(request.getPlanStartTime(), request.getPlanEndTime(), "计划开始时间不能晚于计划结束时间");
        orderService.requireOrder(request.getOrderId());
        orderService.validateOrderItemForOrder(request.getOrderId(), request.getOrderItemId());
        orderService.ensureBatchAllocatedToOrderItem(request.getOrderId(), request.getOrderItemId(), request.getBatchId());
        validateActivePlanConflicts(request);
        BatchInfo batch = batchService.requireBatch(request.getBatchId());
        ProcessRoute route = processRouteService.requireRoute(request.getRouteId());
        if (!Objects.equals(route.getIsActive(), 1)) {
            throw new BusinessException(422, "工艺路线未启用，无法用于排产");
        }
        List<RouteStep> routeSteps = processRouteService.listRequiredRouteSteps(route.getRouteId());

        ProductionPlan plan = new ProductionPlan();
        plan.setOrderId(request.getOrderId());
        plan.setOrderItemId(request.getOrderItemId());
        plan.setBatchId(batch.getBatchId());
        plan.setRouteId(route.getRouteId());
        plan.setPlanStartTime(request.getPlanStartTime());
        plan.setPlanEndTime(request.getPlanEndTime());
        plan.setStatus("DRAFT");
        plan.setCreateTime(LocalDateTime.now());
        plan.setRemark(request.getRemark());
        productionPlanMapper.insert(plan);

        List<PlanStep> generatedSteps = generatePlanSteps(plan, batch, routeSteps);
        generatedSteps.forEach(planStepMapper::insert);

        if (request.getPlanEndTime() != null) {
            LocalDateTime actualEnd = generatedSteps.isEmpty() ? request.getPlanStartTime() : generatedSteps.get(generatedSteps.size() - 1).getPlanEndTime();
            if (actualEnd != null && actualEnd.isAfter(request.getPlanEndTime())) {
                throw new BusinessException(422, "生成的工序计划超出了指定的计划结束时间");
            }
        }
        return Map.of(
                "planId", plan.getPlanId(),
                "planSteps", generatedSteps.size()
        );
    }

    @Transactional
    public ProductionPlan update(Long planId, ProductionPlanUpdateRequest request) {
        ProductionPlan plan = requirePlan(planId);
        validateTimeRange(request.getPlanStartTime(), request.getPlanEndTime(), "计划开始时间不能晚于计划结束时间");
        if (request.getPlanStartTime() != null) {
            plan.setPlanStartTime(request.getPlanStartTime());
        }
        if (request.getPlanEndTime() != null) {
            plan.setPlanEndTime(request.getPlanEndTime());
        }
        if (StringUtils.hasText(request.getStatus())) {
            assertPlanStatus(request.getStatus());
            plan.setStatus(request.getStatus());
        }
        if (request.getRemark() != null) {
            plan.setRemark(request.getRemark());
        }
        productionPlanMapper.updateById(plan);
        return plan;
    }

    @Transactional
    public ProductionPlan patchStatus(Long planId, StatusPatchRequest request) {
        assertPlanStatus(request.getStatus());
        ProductionPlan plan = requirePlan(planId);
        plan.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getReason())) {
            plan.setRemark(AuditRemarkUtils.append(plan.getRemark(), "PLAN_STATUS", request.getReason()));
        }
        productionPlanMapper.updateById(plan);
        return plan;
    }

    @Transactional
    public Map<String, Object> reschedule(Long planId, PlanRescheduleRequest request) {
        ProductionPlan plan = requirePlan(planId);
        List<PlanStep> steps = findPlanSteps(planId);
        LocalDateTime startTime = request.getStartTime() == null ? plan.getPlanStartTime() : request.getStartTime();
        LocalDateTime cursor = startTime;
        for (PlanStep step : steps) {
            step.setPlanStartTime(cursor);
            BigDecimal hours = step.getPlanHours() == null ? BigDecimal.ZERO : step.getPlanHours();
            LocalDateTime endTime = cursor.plusMinutes(hours.multiply(BigDecimal.valueOf(60)).longValue());
            step.setPlanEndTime(endTime);
            step.setRemark(AuditRemarkUtils.append(step.getRemark(), "RESCHEDULE", request.getRescheduleReason()));
            planStepMapper.updateById(step);
            cursor = endTime;
        }
        plan.setPlanStartTime(startTime);
        plan.setPlanEndTime(cursor);
        plan.setRemark(AuditRemarkUtils.append(plan.getRemark(), "RESCHEDULE", request.getRescheduleReason()));
        productionPlanMapper.updateById(plan);
        return Map.of(
                "planId", planId,
                "newStartTime", plan.getPlanStartTime(),
                "newEndTime", plan.getPlanEndTime()
        );
    }

    public Map<String, Object> getGantt(Long planId) {
        requirePlan(planId);
        List<GanttTaskVo> tasks = buildPlanStepVos(findPlanSteps(planId)).stream().map(item -> {
            GanttTaskVo task = new GanttTaskVo();
            task.setPlanStepId(item.getPlanStepId());
            task.setStepName(item.getStepName());
            task.setMachineName(item.getMachineName());
            task.setStart(item.getPlanStartTime());
            task.setEnd(item.getPlanEndTime());
            task.setStatus(item.getStatus());
            return task;
        }).toList();
        return Map.of(
                "planId", planId,
                "tasks", tasks
        );
    }

    public ProductionPlan requirePlan(Long planId) {
        ProductionPlan plan = productionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "生产计划不存在");
        }
        return plan;
    }

    public List<PlanStep> findPlanSteps(Long planId) {
        return planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                .eq(PlanStep::getPlanId, planId)
                .orderByAsc(PlanStep::getSequenceNo));
    }

    public List<PlanStepVo> buildPlanStepVos(List<PlanStep> planSteps) {
        if (planSteps.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ProcessStep> stepMap = planSteps.stream()
                .map(PlanStep::getStepId)
                .distinct()
                .map(processStepService::requireStep)
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
        Map<Long, Machine> machineMap = planSteps.stream()
                .map(PlanStep::getMachineId)
                .filter(Objects::nonNull)
                .distinct()
                .map(machineService::requireMachine)
                .collect(Collectors.toMap(Machine::getMachineId, Function.identity()));
        return planSteps.stream().map(item -> {
            PlanStepVo vo = new PlanStepVo();
            vo.setPlanStepId(item.getPlanStepId());
            vo.setPlanId(item.getPlanId());
            vo.setStepId(item.getStepId());
            ProcessStep step = stepMap.get(item.getStepId());
            if (step != null) {
                vo.setStepName(step.getStepName());
            }
            vo.setMachineId(item.getMachineId());
            Machine machine = machineMap.get(item.getMachineId());
            if (machine != null) {
                vo.setMachineName(machine.getMachineName());
            }
            vo.setPlanStartTime(item.getPlanStartTime());
            vo.setPlanEndTime(item.getPlanEndTime());
            vo.setPlanHours(item.getPlanHours());
            vo.setSequenceNo(item.getSequenceNo());
            vo.setStatus(item.getStatus());
            vo.setRemark(item.getRemark());
            return vo;
        }).toList();
    }

    private List<PlanStep> generatePlanSteps(ProductionPlan plan, BatchInfo batch, List<RouteStep> routeSteps) {
        List<PlanStep> generated = new ArrayList<>();
        LocalDateTime cursor = plan.getPlanStartTime();
        for (RouteStep routeStep : routeSteps) {
            ProcessStep step = processStepService.requireStep(routeStep.getStepId());
            List<StepMachineCapability> capabilities = capabilityService.findActiveCapabilities(step.getStepId()).stream()
                    .filter(item -> capabilityService.supportsCapability(item, batch.getWidth(), batch.getWeight()))
                    .toList();
            ScheduleMachineAssignment assignment = schedulingAlgorithmService.chooseEarliestFinishMachine(
                    step,
                    batch,
                    cursor,
                    capabilities
            );

            PlanStep planStep = new PlanStep();
            planStep.setPlanId(plan.getPlanId());
            planStep.setStepId(step.getStepId());
            planStep.setMachineId(assignment.getMachineId());
            planStep.setPlanStartTime(assignment.getStartTime());
            planStep.setPlanEndTime(assignment.getEndTime());
            planStep.setPlanHours(assignment.getPlanHours());
            planStep.setSequenceNo(routeStep.getSortOrder());
            planStep.setStatus("PENDING");
            planStep.setRemark(assignment.getReason());
            generated.add(planStep);
            cursor = assignment.getEndTime();
        }
        if (!generated.isEmpty()) {
            plan.setPlanEndTime(generated.get(generated.size() - 1).getPlanEndTime());
            productionPlanMapper.updateById(plan);
        }
        return generated;
    }

    private Map<Long, BatchInfo> fetchBatchMap(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct()
                .map(batchService::requireBatch)
                .collect(Collectors.toMap(BatchInfo::getBatchId, Function.identity()));
    }

    private Map<Long, ProcessRoute> fetchRouteMap(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct()
                .map(processRouteService::requireRoute)
                .collect(Collectors.toMap(ProcessRoute::getRouteId, Function.identity()));
    }

    private ProductionPlanListVo toListVo(ProductionPlan plan,
                                          OrderInfo order,
                                          OrderItem orderItem,
                                          BatchInfo batch,
                                          ProcessRoute route) {
        ProductionPlanListVo vo = new ProductionPlanListVo();
        vo.setPlanId(plan.getPlanId());
        vo.setOrderId(plan.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setCustomerName(order == null ? null : order.getCustomerName());
        vo.setOrderItemId(plan.getOrderItemId());
        vo.setProductCode(orderItem == null ? null : orderItem.getProductCode());
        vo.setProductName(orderItem == null ? null : orderItem.getProductName());
        vo.setSpecification(orderItem == null ? null : orderItem.getSpecification());
        vo.setColor(orderItem == null ? null : orderItem.getColor());
        vo.setBatchId(plan.getBatchId());
        vo.setBatchNo(batch == null ? null : batch.getBatchNo());
        vo.setRouteId(plan.getRouteId());
        vo.setRouteName(route == null ? null : route.getRouteName());
        vo.setPlanStartTime(plan.getPlanStartTime());
        vo.setPlanEndTime(plan.getPlanEndTime());
        vo.setStatus(plan.getStatus());
        vo.setCreateTime(plan.getCreateTime());
        vo.setRemark(plan.getRemark());
        return vo;
    }

    private void assertPlanStatus(String status) {
        if (!PLAN_STATUSES.contains(status)) {
            throw new BusinessException(422, "不支持的生产计划状态: " + status);
        }
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }

    private void validateActivePlanConflicts(ProductionPlanCreateRequest request) {
        List<ProductionPlan> activePlans = productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getBatchId, request.getBatchId())
                .in(ProductionPlan::getStatus, ACTIVE_PLAN_STATUSES)
                .orderByDesc(ProductionPlan::getCreateTime));
        if (activePlans.isEmpty()) {
            return;
        }
        ProductionPlan duplicatedPlan = activePlans.stream()
                .filter(plan -> Objects.equals(plan.getOrderId(), request.getOrderId())
                        && Objects.equals(plan.getOrderItemId(), request.getOrderItemId())
                        && Objects.equals(plan.getBatchId(), request.getBatchId()))
                .findFirst()
                .orElse(null);
        if (duplicatedPlan != null) {
            throw new BusinessException(409, "该订单明细批次已存在活跃生产计划，请勿重复创建");
        }
        ProductionPlan conflictPlan = activePlans.get(0);
        throw new BusinessException(409, "该批次已有活跃生产计划，计划ID：" + conflictPlan.getPlanId() + "，请先处理后再创建");
    }
}
