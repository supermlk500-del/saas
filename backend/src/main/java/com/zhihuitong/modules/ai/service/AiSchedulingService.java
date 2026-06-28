package com.zhihuitong.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import com.zhihuitong.common.enums.DeviceStatus;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.modules.ai.dto.ScheduleSuggestionApplyRequest;
import com.zhihuitong.modules.ai.entity.AiScheduleRecord;
import com.zhihuitong.modules.ai.mapper.AiScheduleRecordMapper;
import com.zhihuitong.modules.ai.vo.AiScheduleOptionVo;
import com.zhihuitong.modules.ai.vo.AiScheduleStepVo;
import com.zhihuitong.modules.ai.vo.AiScheduleSuggestionVo;
import com.zhihuitong.modules.ai.vo.AiScheduleRecordSummaryVo;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.plan.service.ProductionPlanService;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.service.MachineService;
import com.zhihuitong.modules.process.service.ProcessStepService;
import com.zhihuitong.modules.process.service.StepMachineCapabilityService;
import com.zhihuitong.security.service.DataScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiSchedulingService {

    private static final Set<String> STRATEGIES = Set.of("DELIVERY", "UTILIZATION", "COST");

    static {
        Loader.loadNativeLibraries();
    }

    private final ProductionPlanService productionPlanService;
    private final ProductionPlanMapper productionPlanMapper;
    private final PlanStepMapper planStepMapper;
    private final BatchService batchService;
    private final OrderService orderService;
    private final ProcessStepService processStepService;
    private final MachineService machineService;
    private final StepMachineCapabilityService capabilityService;
    private final DeepSeekExplanationService explanationService;
    private final DataScopeService dataScopeService;
    private final AiScheduleRecordMapper scheduleRecordMapper;
    private final ObjectMapper objectMapper;

    public AiSchedulingService(ProductionPlanService productionPlanService,
                               ProductionPlanMapper productionPlanMapper,
                               PlanStepMapper planStepMapper,
                               BatchService batchService,
                               OrderService orderService,
                               ProcessStepService processStepService,
                               MachineService machineService,
                               StepMachineCapabilityService capabilityService,
                               DeepSeekExplanationService explanationService,
                               DataScopeService dataScopeService,
                               AiScheduleRecordMapper scheduleRecordMapper,
                               ObjectMapper objectMapper) {
        this.productionPlanService = productionPlanService;
        this.productionPlanMapper = productionPlanMapper;
        this.planStepMapper = planStepMapper;
        this.batchService = batchService;
        this.orderService = orderService;
        this.processStepService = processStepService;
        this.machineService = machineService;
        this.capabilityService = capabilityService;
        this.explanationService = explanationService;
        this.dataScopeService = dataScopeService;
        this.scheduleRecordMapper = scheduleRecordMapper;
        this.objectMapper = objectMapper;
    }

    public AiScheduleSuggestionVo suggest(Long planId) {
        ProductionPlan plan = productionPlanService.requirePlan(planId);
        dataScopeService.assertAccessible(plan.getDeptId(), plan.getCreatedBy());
        List<PlanStep> planSteps = productionPlanService.findPlanSteps(planId);
        if (planSteps.isEmpty()) {
            throw new BusinessException(422, "生产计划尚未生成工序，无法计算排产建议");
        }
        BatchInfo batch = batchService.requireBatch(plan.getBatchId());
        OrderInfo order = plan.getOrderId() == null ? null : orderService.requireOrder(plan.getOrderId());

        AiScheduleSuggestionVo result = new AiScheduleSuggestionVo();
        result.setPlanId(planId);
        result.setOrderNo(order == null ? null : order.getOrderNo());
        result.setBatchNo(batch.getBatchNo());
        result.setDeliveryDate(order == null ? null : order.getDeliveryDate());
        List<AiScheduleOptionVo> options = List.of(
                solve(plan, planSteps, batch, order, "DELIVERY", false),
                solve(plan, planSteps, batch, order, "UTILIZATION", false),
                solve(plan, planSteps, batch, order, "COST", false)
        );
        List<CompletableFuture<Void>> explanationTasks = options.stream()
                .map(option -> CompletableFuture.runAsync(() -> explainOption(option)))
                .toList();
        CompletableFuture.allOf(explanationTasks.toArray(CompletableFuture[]::new)).join();
        result.setOptions(options);
        if (!result.getOptions().isEmpty()) {
            result.setExplanationModel(result.getOptions().get(0).getModel());
        }
        persistSuggestion(result);
        return result;
    }

    public List<AiScheduleRecordSummaryVo> listRecords(Long planId) {
        List<AiScheduleRecord> records = scheduleRecordMapper.selectList(dataScopeService.apply(
                        Wrappers.<AiScheduleRecord>lambdaQuery(),
                        AiScheduleRecord::getDeptId,
                        AiScheduleRecord::getCreatedBy)
                .eq(planId != null, AiScheduleRecord::getPlanId, planId)
                .orderByDesc(AiScheduleRecord::getCreateTime)
                .last("LIMIT 100"));
        return records.stream().map(this::toSummary).toList();
    }

    public AiScheduleSuggestionVo getRecord(Long recordId) {
        AiScheduleRecord record = requireRecord(recordId);
        return deserializeRecord(record);
    }

    @Transactional
    public Map<String, Object> apply(Long planId, ScheduleSuggestionApplyRequest request) {
        String strategy = normalizeStrategy(request.getStrategy());
        ProductionPlan sourcePlan = productionPlanService.requirePlan(planId);
        dataScopeService.assertAccessible(sourcePlan.getDeptId(), sourcePlan.getCreatedBy());
        AiScheduleRecord sourceRecord = request.getRecordId() == null ? null : requireRecord(request.getRecordId());
        if (sourceRecord != null && !Objects.equals(sourceRecord.getPlanId(), planId)) {
            throw new BusinessException(422, "排产记录与当前生产计划不匹配");
        }
        AiScheduleOptionVo option;
        if (sourceRecord != null) {
            option = deserializeRecord(sourceRecord).getOptions().stream()
                    .filter(item -> strategy.equals(item.getStrategy()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(422, "历史记录中不存在指定方案"));
        } else {
            BatchInfo batch = batchService.requireBatch(sourcePlan.getBatchId());
            OrderInfo order = sourcePlan.getOrderId() == null ? null : orderService.requireOrder(sourcePlan.getOrderId());
            option = solve(sourcePlan, productionPlanService.findPlanSteps(planId), batch, order, strategy, false);
        }
        if (!"OPTIMAL".equals(option.getSolverStatus()) && !"FEASIBLE".equals(option.getSolverStatus())) {
            throw new BusinessException(422, "当前策略没有可执行解");
        }
        Map<Long, PlanStep> stepMap = productionPlanService.findPlanSteps(planId).stream()
                .collect(Collectors.toMap(PlanStep::getPlanStepId, Function.identity()));
        String auditText = strategy + (StringUtils.hasText(request.getConfirmationRemark())
                ? ", " + request.getConfirmationRemark().trim() : "");
        for (AiScheduleStepVo suggestion : option.getSteps()) {
            PlanStep step = stepMap.get(suggestion.getPlanStepId());
            if (step == null) {
                continue;
            }
            step.setMachineId(suggestion.getMachineId());
            step.setPlanStartTime(suggestion.getStartTime());
            step.setPlanEndTime(suggestion.getEndTime());
            step.setPlanHours(suggestion.getPredictedHours());
            step.setRemark(AuditRemarkUtils.append(step.getRemark(), "AI_SCHEDULE_CONFIRMED", auditText));
            planStepMapper.updateById(step);
        }
        ProductionPlan plan = productionPlanService.requirePlan(planId);
        plan.setPlanStartTime(option.getStartTime());
        plan.setPlanEndTime(option.getEndTime());
        plan.setRemark(AuditRemarkUtils.append(plan.getRemark(), "AI_SCHEDULE_CONFIRMED", auditText));
        productionPlanMapper.updateById(plan);
        if (sourceRecord != null) {
            sourceRecord.setStatus("APPLIED");
            sourceRecord.setSelectedStrategy(strategy);
            sourceRecord.setAppliedTime(LocalDateTime.now());
            scheduleRecordMapper.updateById(sourceRecord);
        }
        return Map.of(
                "planId", planId,
                "strategy", strategy,
                "startTime", option.getStartTime(),
                "endTime", option.getEndTime(),
                "updatedSteps", option.getSteps().size()
        );
    }

    private void persistSuggestion(AiScheduleSuggestionVo suggestion) {
        try {
            AiScheduleRecord record = new AiScheduleRecord();
            record.setPlanId(suggestion.getPlanId());
            record.setStatus("GENERATED");
            record.setSnapshotJson(objectMapper.writeValueAsString(suggestion));
            record.setDeptId(dataScopeService.currentDeptId());
            record.setCreatedBy(dataScopeService.currentUserId());
            record.setCreateTime(LocalDateTime.now());
            scheduleRecordMapper.insert(record);
            suggestion.setRecordId(record.getRecordId());
            suggestion.setRecordStatus(record.getStatus());
            suggestion.setCreateTime(record.getCreateTime());
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "排产方案快照保存失败");
        }
    }

    private AiScheduleRecord requireRecord(Long recordId) {
        AiScheduleRecord record = scheduleRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "AI 排产记录不存在");
        }
        dataScopeService.assertAccessible(record.getDeptId(), record.getCreatedBy());
        return record;
    }

    private AiScheduleSuggestionVo deserializeRecord(AiScheduleRecord record) {
        try {
            AiScheduleSuggestionVo suggestion = objectMapper.readValue(record.getSnapshotJson(), AiScheduleSuggestionVo.class);
            suggestion.setRecordId(record.getRecordId());
            suggestion.setRecordStatus(record.getStatus());
            suggestion.setSelectedStrategy(record.getSelectedStrategy());
            suggestion.setCreateTime(record.getCreateTime());
            suggestion.setAppliedTime(record.getAppliedTime());
            return suggestion;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "AI 排产记录解析失败");
        }
    }

    private AiScheduleRecordSummaryVo toSummary(AiScheduleRecord record) {
        AiScheduleSuggestionVo snapshot = deserializeRecord(record);
        AiScheduleRecordSummaryVo vo = new AiScheduleRecordSummaryVo();
        vo.setRecordId(record.getRecordId());
        vo.setPlanId(record.getPlanId());
        vo.setOrderNo(snapshot.getOrderNo());
        vo.setBatchNo(snapshot.getBatchNo());
        vo.setStatus(record.getStatus());
        vo.setSelectedStrategy(record.getSelectedStrategy());
        vo.setCreateTime(record.getCreateTime());
        vo.setAppliedTime(record.getAppliedTime());
        return vo;
    }

    private AiScheduleOptionVo solve(ProductionPlan plan,
                                     List<PlanStep> planSteps,
                                     BatchInfo batch,
                                     OrderInfo order,
                                     String strategy,
                                     boolean includeExplanation) {
        LocalDateTime baseTime = plan.getPlanStartTime() == null ? LocalDateTime.now() : plan.getPlanStartTime();
        List<PreparedStep> preparedSteps = prepareSteps(planSteps, batch, strategy);
        long totalDuration = preparedSteps.stream().mapToLong(PreparedStep::durationMinutes).sum();
        long horizon = Math.max(totalDuration + 7 * 24 * 60L, 24 * 60L);

        CpModel model = new CpModel();
        Map<Long, List<IntervalVar>> intervalsByMachine = new HashMap<>();
        List<TaskVars> taskVars = new ArrayList<>();
        for (int index = 0; index < preparedSteps.size(); index++) {
            PreparedStep prepared = preparedSteps.get(index);
            IntVar start = model.newIntVar(0, horizon, "start_" + index);
            IntVar end = model.newIntVar(0, horizon, "end_" + index);
            IntervalVar interval = model.newIntervalVar(
                    start,
                    model.newConstant(prepared.durationMinutes()),
                    end,
                    "task_" + index
            );
            intervalsByMachine.computeIfAbsent(prepared.machine().getMachineId(), ignored -> new ArrayList<>()).add(interval);
            taskVars.add(new TaskVars(prepared, start, end));
            if (index > 0) {
                model.addGreaterOrEqual(start, taskVars.get(index - 1).end());
            }
        }

        addExistingOccupancy(model, intervalsByMachine, plan.getPlanId(), baseTime, horizon);
        intervalsByMachine.values().forEach(model::addNoOverlap);
        IntVar makespan = taskVars.get(taskVars.size() - 1).end();
        model.minimize(makespan);

        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(5.0);
        solver.getParameters().setNumSearchWorkers(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        CpSolverStatus status = solver.solve(model);

        AiScheduleOptionVo option = new AiScheduleOptionVo();
        option.setStrategy(strategy);
        option.setStrategyName(strategyName(strategy));
        option.setSolverStatus(status.name());
        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE) {
            option.setExplanation("现有机台占用与工序约束下未找到可执行方案，请检查设备能力和计划时间窗口。");
            option.setModel("deterministic-fallback");
            return option;
        }

        List<AiScheduleStepVo> solvedSteps = taskVars.stream().map(task -> toStepVo(
                task.prepared(),
                baseTime.plusMinutes(solver.value(task.start())),
                baseTime.plusMinutes(solver.value(task.end()))
        )).toList();
        LocalDateTime endTime = solvedSteps.get(solvedSteps.size() - 1).getEndTime();
        long delayMinutes = order == null || order.getDeliveryDate() == null || !endTime.isAfter(order.getDeliveryDate())
                ? 0 : ChronoUnit.MINUTES.between(order.getDeliveryDate(), endTime);
        int machineChanges = 0;
        for (int index = 1; index < solvedSteps.size(); index++) {
            if (!Objects.equals(solvedSteps.get(index - 1).getMachineId(), solvedSteps.get(index).getMachineId())) {
                machineChanges++;
            }
        }
        int utilizationScore = (int) Math.round(preparedSteps.stream()
                .mapToInt(item -> machineAvailabilityScore(item.machine()))
                .average().orElse(0));

        option.setStartTime(solvedSteps.get(0).getStartTime());
        option.setEndTime(endTime);
        option.setTotalMinutes(ChronoUnit.MINUTES.between(option.getStartTime(), option.getEndTime()));
        option.setDelayMinutes(delayMinutes);
        option.setMachineChanges(machineChanges);
        option.setEstimatedUtilizationScore(utilizationScore);
        option.setSteps(solvedSteps);

        if (includeExplanation) {
            String fallback = buildScheduleFallback(option);
            DeepSeekExplanationService.ExplanationResult explanation = explanationService.explain(
                    "你是纺织生产计划分析助手。只解释给定的 OR-Tools 排产结果，不重新计算、不改动机台和时间。"
                            + "用简洁中文说明方案优势、风险、延期情况和人工确认重点。",
                    option,
                    fallback
            );
            option.setExplanation(explanation.content());
            option.setModel(explanation.model());
            option.setAiGenerated(explanation.aiGenerated());
            option.setFallbackReason(explanation.fallbackReason());
        }
        return option;
    }

    private void explainOption(AiScheduleOptionVo option) {
        if (!"OPTIMAL".equals(option.getSolverStatus()) && !"FEASIBLE".equals(option.getSolverStatus())) {
            return;
        }
        String fallback = buildScheduleFallback(option);
        DeepSeekExplanationService.ExplanationResult explanation = explanationService.explain(
                "你是纺织生产计划分析助手。只解释给定的 OR-Tools 排产结果，不重新计算、不改动机台和时间。"
                        + "用简洁中文说明方案优势、风险、延期情况和人工确认重点。",
                option,
                fallback
        );
        option.setExplanation(explanation.content());
        option.setModel(explanation.model());
        option.setAiGenerated(explanation.aiGenerated());
        option.setFallbackReason(explanation.fallbackReason());
    }

    private List<PreparedStep> prepareSteps(List<PlanStep> planSteps, BatchInfo batch, String strategy) {
        Map<Long, Long> currentLoads = planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                        .in(PlanStep::getStatus, List.of("PENDING", "READY", "RUNNING")))
                .stream()
                .filter(item -> item.getMachineId() != null)
                .collect(Collectors.groupingBy(PlanStep::getMachineId, Collectors.counting()));
        List<PreparedStep> result = new ArrayList<>();
        for (PlanStep planStep : planSteps) {
            ProcessStep processStep = processStepService.requireStep(planStep.getStepId());
            List<MachineCandidate> candidates = capabilityService.findActiveCapabilities(planStep.getStepId()).stream()
                    .filter(capability -> capabilityService.supportsCapability(capability, batch.getWidth(), batch.getWeight()))
                    .map(capability -> {
                        Machine machine = machineService.requireMachine(capability.getMachineId());
                        return new MachineCandidate(machine, capability,
                                scoreCandidate(strategy, machine, capability, currentLoads.getOrDefault(machine.getMachineId(), 0L),
                                        Objects.equals(planStep.getMachineId(), machine.getMachineId())));
                    })
                    .filter(item -> item.machine().getStatus() == null
                            || item.machine().getStatus() == DeviceStatus.IDLE.getCode()
                            || item.machine().getStatus() == DeviceStatus.RUNNING.getCode())
                    .sorted(Comparator.comparingInt(MachineCandidate::score).reversed()
                            .thenComparing(item -> item.machine().getMachineCode(), Comparator.nullsLast(String::compareTo)))
                    .toList();
            if (candidates.isEmpty()) {
                throw new BusinessException(422, "工序“" + processStep.getStepName() + "”没有满足门幅和重量约束的可用机台");
            }
            MachineCandidate selected = candidates.get(0);
            long duration = resolveDurationMinutes(planStep, processStep, selected.capability(), strategy);
            String reason = "能力约束匹配；" + DeviceStatus.fromCode(selected.machine().getStatus())
                    + "；策略评分 " + selected.score();
            result.add(new PreparedStep(planStep, processStep, selected.machine(), selected.capability(),
                    selected.score(), reason, duration));
        }
        return result;
    }

    private int scoreCandidate(String strategy,
                               Machine machine,
                               StepMachineCapability capability,
                               long activeLoad,
                               boolean currentlyAssigned) {
        int availability = machineAvailabilityScore(machine);
        int speed = capability.getMaxSpeed() == null ? 0 : capability.getMaxSpeed().min(BigDecimal.valueOf(100)).intValue();
        int loadPenalty = (int) Math.min(40, activeLoad * 8);
        return switch (strategy) {
            case "DELIVERY" -> availability * 2 + speed * 2 - loadPenalty;
            case "UTILIZATION" -> availability * 3 + Math.max(0, 50 - loadPenalty) + speed;
            case "COST" -> availability * 2 + (currentlyAssigned ? 60 : 0) + speed - loadPenalty / 2;
            default -> availability + speed;
        };
    }

    private int machineAvailabilityScore(Machine machine) {
        if (machine.getStatus() == null) {
            return 20;
        }
        if (machine.getStatus() == DeviceStatus.IDLE.getCode()) {
            return 100;
        }
        if (machine.getStatus() == DeviceStatus.RUNNING.getCode()) {
            return 55;
        }
        return 0;
    }

    private long resolveDurationMinutes(PlanStep planStep,
                                        ProcessStep processStep,
                                        StepMachineCapability capability,
                                        String strategy) {
        BigDecimal hours = planStep.getPlanHours() != null
                ? planStep.getPlanHours()
                : (processStep.getDefaultHours() == null ? BigDecimal.ONE : processStep.getDefaultHours());
        BigDecimal factor = BigDecimal.ONE;
        if ("DELIVERY".equals(strategy) && capability.getMaxSpeed() != null) {
            factor = BigDecimal.valueOf(0.9);
        } else if ("COST".equals(strategy)) {
            factor = BigDecimal.valueOf(1.05);
        }
        return Math.max(15, hours.multiply(factor).multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.CEILING).longValue());
    }

    private void addExistingOccupancy(CpModel model,
                                      Map<Long, List<IntervalVar>> intervalsByMachine,
                                      Long currentPlanId,
                                      LocalDateTime baseTime,
                                      long horizon) {
        if (intervalsByMachine.isEmpty()) {
            return;
        }
        List<PlanStep> occupiedSteps = planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                .ne(PlanStep::getPlanId, currentPlanId)
                .in(PlanStep::getMachineId, intervalsByMachine.keySet())
                .isNotNull(PlanStep::getPlanStartTime)
                .isNotNull(PlanStep::getPlanEndTime)
                .notIn(PlanStep::getStatus, List.of("FINISHED", "CANCELLED")));
        int index = 0;
        for (PlanStep occupied : occupiedSteps) {
            long start = Math.max(0, ChronoUnit.MINUTES.between(baseTime, occupied.getPlanStartTime()));
            long end = Math.min(horizon, ChronoUnit.MINUTES.between(baseTime, occupied.getPlanEndTime()));
            if (end <= 0 || start >= horizon || end <= start) {
                continue;
            }
            IntVar fixedStart = model.newIntVar(start, start, "busy_start_" + index);
            IntVar fixedEnd = model.newIntVar(end, end, "busy_end_" + index);
            IntervalVar interval = model.newIntervalVar(
                    fixedStart,
                    model.newConstant(end - start),
                    fixedEnd,
                    "busy_" + index
            );
            intervalsByMachine.computeIfAbsent(occupied.getMachineId(), ignored -> new ArrayList<>()).add(interval);
            index++;
        }
    }

    private AiScheduleStepVo toStepVo(PreparedStep prepared, LocalDateTime startTime, LocalDateTime endTime) {
        AiScheduleStepVo vo = new AiScheduleStepVo();
        vo.setPlanStepId(prepared.planStep().getPlanStepId());
        vo.setStepId(prepared.processStep().getStepId());
        vo.setStepName(prepared.processStep().getStepName());
        vo.setMachineId(prepared.machine().getMachineId());
        vo.setMachineCode(prepared.machine().getMachineCode());
        vo.setMachineName(prepared.machine().getMachineName());
        vo.setRecommendationScore(prepared.score());
        vo.setRecommendationReason(prepared.reason());
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        vo.setPredictedHours(BigDecimal.valueOf(prepared.durationMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        return vo;
    }

    private String buildScheduleFallback(AiScheduleOptionVo option) {
        String delay = option.getDelayMinutes() > 0
                ? "预计延期约 " + Math.ceil(option.getDelayMinutes() / 60.0) + " 小时"
                : "预计可按期完成";
        return option.getStrategyName() + "方案由 OR-Tools 计算，" + delay
                + "；总历时约 " + Math.ceil(option.getTotalMinutes() / 60.0) + " 小时，"
                + "机台切换 " + option.getMachineChanges() + " 次。请重点确认机台状态、物料到位和工艺参数。";
    }

    private String normalizeStrategy(String strategy) {
        String normalized = strategy == null ? "" : strategy.trim().toUpperCase(Locale.ROOT);
        if (!STRATEGIES.contains(normalized)) {
            throw new BusinessException(422, "strategy 仅支持 DELIVERY、UTILIZATION 或 COST");
        }
        return normalized;
    }

    private String strategyName(String strategy) {
        return switch (strategy) {
            case "DELIVERY" -> "交期优先";
            case "UTILIZATION" -> "设备利用率优先";
            case "COST" -> "成本优先";
            default -> strategy;
        };
    }

    private record MachineCandidate(Machine machine, StepMachineCapability capability, int score) {
    }

    private record PreparedStep(PlanStep planStep,
                                ProcessStep processStep,
                                Machine machine,
                                StepMachineCapability capability,
                                int score,
                                String reason,
                                long durationMinutes) {
    }

    private record TaskVars(PreparedStep prepared, IntVar start, IntVar end) {
    }
}
