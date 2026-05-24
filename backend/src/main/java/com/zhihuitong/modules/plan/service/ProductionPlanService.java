package com.zhihuitong.modules.plan.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.enums.DeviceStatus;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchService;
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
import java.util.Comparator;
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

    private final ProductionPlanMapper productionPlanMapper;
    private final PlanStepMapper planStepMapper;
    private final BatchService batchService;
    private final ProcessRouteService processRouteService;
    private final ProcessStepService processStepService;
    private final MachineService machineService;
    private final StepMachineCapabilityService capabilityService;

    public ProductionPlanService(ProductionPlanMapper productionPlanMapper,
                                 PlanStepMapper planStepMapper,
                                 BatchService batchService,
                                 ProcessRouteService processRouteService,
                                 ProcessStepService processStepService,
                                 MachineService machineService,
                                 StepMachineCapabilityService capabilityService) {
        this.productionPlanMapper = productionPlanMapper;
        this.planStepMapper = planStepMapper;
        this.batchService = batchService;
        this.processRouteService = processRouteService;
        this.processStepService = processStepService;
        this.machineService = machineService;
        this.capabilityService = capabilityService;
    }

    public TableDataInfo<ProductionPlanListVo> list(ProductionPlanQuery query) {
        validateTimeRange(query.getPlanStartFrom(), query.getPlanStartTo(), "planStartFrom must be earlier than or equal to planStartTo");
        Page<ProductionPlan> page = productionPlanMapper.selectPage(query.toPage(), Wrappers.<ProductionPlan>lambdaQuery()
                .eq(query.getBatchId() != null, ProductionPlan::getBatchId, query.getBatchId())
                .eq(query.getRouteId() != null, ProductionPlan::getRouteId, query.getRouteId())
                .eq(StringUtils.hasText(query.getStatus()), ProductionPlan::getStatus, query.getStatus())
                .ge(query.getPlanStartFrom() != null, ProductionPlan::getPlanStartTime, query.getPlanStartFrom())
                .le(query.getPlanStartTo() != null, ProductionPlan::getPlanStartTime, query.getPlanStartTo())
                .orderByDesc(ProductionPlan::getCreateTime));
        List<ProductionPlan> records = page.getRecords();
        Map<Long, BatchInfo> batchMap = fetchBatchMap(records.stream().map(ProductionPlan::getBatchId).toList());
        Map<Long, ProcessRoute> routeMap = fetchRouteMap(records.stream().map(ProductionPlan::getRouteId).toList());
        List<ProductionPlanListVo> rows = records.stream().map(item -> toListVo(item, batchMap.get(item.getBatchId()), routeMap.get(item.getRouteId()))).toList();
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public ProductionPlanDetailVo getDetail(Long planId) {
        ProductionPlan plan = requirePlan(planId);
        ProductionPlanDetailVo detail = new ProductionPlanDetailVo();
        detail.setPlanId(plan.getPlanId());
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
        validateTimeRange(request.getPlanStartTime(), request.getPlanEndTime(), "planStartTime must be earlier than or equal to planEndTime");
        BatchInfo batch = batchService.requireBatch(request.getBatchId());
        ProcessRoute route = processRouteService.requireRoute(request.getRouteId());
        if (!Objects.equals(route.getIsActive(), 1)) {
            throw new BusinessException(422, "Process route must be active");
        }
        List<RouteStep> routeSteps = processRouteService.listRequiredRouteSteps(route.getRouteId());

        ProductionPlan plan = new ProductionPlan();
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
                throw new BusinessException(422, "Generated plan steps exceed the provided planEndTime");
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
        validateTimeRange(request.getPlanStartTime(), request.getPlanEndTime(), "planStartTime must be earlier than or equal to planEndTime");
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
            throw new BusinessException(404, "Production plan not found");
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
            BigDecimal planHours = resolvePlanHours(step);
            LocalDateTime endTime = calculateEndTime(cursor, planHours);

            PlanStep planStep = new PlanStep();
            planStep.setPlanId(plan.getPlanId());
            planStep.setStepId(step.getStepId());
            planStep.setMachineId(pickRecommendedMachine(step.getStepId(), batch));
            planStep.setPlanStartTime(cursor);
            planStep.setPlanEndTime(endTime);
            planStep.setPlanHours(planHours);
            planStep.setSequenceNo(routeStep.getSortOrder());
            planStep.setStatus("PENDING");
            if (planStep.getMachineId() == null) {
                planStep.setRemark("No matching active machine capability was found during plan generation");
            }
            generated.add(planStep);
            cursor = endTime;
        }
        if (!generated.isEmpty()) {
            plan.setPlanEndTime(generated.get(generated.size() - 1).getPlanEndTime());
            productionPlanMapper.updateById(plan);
        }
        return generated;
    }

    private Long pickRecommendedMachine(Long stepId, BatchInfo batch) {
        return capabilityService.findActiveCapabilities(stepId).stream()
                .filter(item -> capabilityService.supportsCapability(item, batch.getWidth(), batch.getWeight()))
                .sorted(Comparator
                        .comparingInt((StepMachineCapability item) -> machineAvailabilityRank(item.getMachineId()))
                        .thenComparing(StepMachineCapability::getMaxSpeed, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StepMachineCapability::getMaxBatchWeight, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(StepMachineCapability::getMachineId)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal resolvePlanHours(ProcessStep step) {
        return step.getDefaultHours() == null ? BigDecimal.ONE : step.getDefaultHours();
    }

    private LocalDateTime calculateEndTime(LocalDateTime startTime, BigDecimal planHours) {
        return startTime.plusMinutes(planHours.multiply(BigDecimal.valueOf(60)).longValue());
    }

    private int machineAvailabilityRank(Long machineId) {
        if (machineId == null) {
            return Integer.MAX_VALUE;
        }
        Machine machine = machineService.requireMachine(machineId);
        if (machine.getStatus() == null) {
            return 10;
        }
        if (machine.getStatus() == DeviceStatus.IDLE.getCode()) {
            return 0;
        }
        if (machine.getStatus() == DeviceStatus.RUNNING.getCode()) {
            return 1;
        }
        if (machine.getStatus() == DeviceStatus.MAINTENANCE.getCode()) {
            return 2;
        }
        return 3;
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

    private ProductionPlanListVo toListVo(ProductionPlan plan, BatchInfo batch, ProcessRoute route) {
        ProductionPlanListVo vo = new ProductionPlanListVo();
        vo.setPlanId(plan.getPlanId());
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
            throw new BusinessException(422, "Unsupported plan status: " + status);
        }
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }
}
