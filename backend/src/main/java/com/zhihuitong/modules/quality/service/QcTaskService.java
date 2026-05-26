package com.zhihuitong.modules.quality.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.mapper.BatchInfoMapper;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.mapper.OrderInfoMapper;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.mapper.MachineMapper;
import com.zhihuitong.modules.process.mapper.ProcessStepMapper;
import com.zhihuitong.modules.quality.dto.QcTaskQuery;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import com.zhihuitong.modules.quality.vo.QcTaskVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QcTaskService {

    private static final String STATUS_PENDING = "待检验";
    private static final String STATUS_PENDING_HANDLE = "待处理";
    private static final String STATUS_COMPLETED = "已检验";

    private final PlanStepMapper planStepMapper;
    private final ProductionPlanMapper productionPlanMapper;
    private final ProcessStepMapper processStepMapper;
    private final MachineMapper machineMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final BatchInfoMapper batchInfoMapper;
    private final QcRecordMapper qcRecordMapper;

    public QcTaskService(PlanStepMapper planStepMapper,
                         ProductionPlanMapper productionPlanMapper,
                         ProcessStepMapper processStepMapper,
                         MachineMapper machineMapper,
                         OrderInfoMapper orderInfoMapper,
                         BatchInfoMapper batchInfoMapper,
                         QcRecordMapper qcRecordMapper) {
        this.planStepMapper = planStepMapper;
        this.productionPlanMapper = productionPlanMapper;
        this.processStepMapper = processStepMapper;
        this.machineMapper = machineMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.batchInfoMapper = batchInfoMapper;
        this.qcRecordMapper = qcRecordMapper;
    }

    public TableDataInfo<QcTaskVo> list(QcTaskQuery query) {
        List<ProductionPlan> scopedPlans = findScopedPlans(query);
        if (requiresPlanScope(query) && scopedPlans.isEmpty()) {
            return TableDataInfo.empty();
        }
        List<PlanStep> planSteps = planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                .in(!scopedPlans.isEmpty(), PlanStep::getPlanId, scopedPlans.stream().map(ProductionPlan::getPlanId).toList())
                .eq(query.getMachineId() != null, PlanStep::getMachineId, query.getMachineId())
                .orderByDesc(PlanStep::getPlanStartTime)
                .orderByDesc(PlanStep::getPlanStepId));
        if (planSteps.isEmpty()) {
            return TableDataInfo.empty();
        }

        Map<Long, ProductionPlan> planMap = buildPlanMap(scopedPlans, planSteps);
        Map<Long, ProcessStep> stepMap = fetchStepMap(planSteps);
        Map<Long, Machine> machineMap = fetchMachineMap(planSteps);
        Map<Long, OrderInfo> orderMap = fetchOrderMap(planMap.values());
        Map<Long, BatchInfo> batchMap = fetchBatchMap(planMap.values());
        Map<Long, QcRecord> latestQcMap = buildLatestQcMap(planSteps);

        List<QcTaskVo> filtered = planSteps.stream()
                .map(step -> toTaskVo(step, planMap, stepMap, machineMap, orderMap, batchMap, latestQcMap))
                .filter(Objects::nonNull)
                .filter(task -> matchesKeyword(task, query.getKeyword()))
                .filter(task -> matchesStatus(task, query.getStatus()))
                .toList();

        return TableDataInfoBuilder.build(paginate(filtered, query), filtered.size());
    }

    private List<ProductionPlan> findScopedPlans(QcTaskQuery query) {
        if (!requiresPlanScope(query)) {
            return Collections.emptyList();
        }
        return productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(query.getPlanId() != null, ProductionPlan::getPlanId, query.getPlanId())
                .eq(query.getOrderId() != null, ProductionPlan::getOrderId, query.getOrderId())
                .eq(query.getOrderItemId() != null, ProductionPlan::getOrderItemId, query.getOrderItemId())
                .eq(query.getBatchId() != null, ProductionPlan::getBatchId, query.getBatchId()));
    }

    private boolean requiresPlanScope(QcTaskQuery query) {
        return query.getPlanId() != null
                || query.getOrderId() != null
                || query.getOrderItemId() != null
                || query.getBatchId() != null;
    }

    private Map<Long, ProductionPlan> buildPlanMap(List<ProductionPlan> scopedPlans, List<PlanStep> planSteps) {
        List<ProductionPlan> plans = scopedPlans;
        if (plans.isEmpty()) {
            List<Long> planIds = planSteps.stream().map(PlanStep::getPlanId).filter(Objects::nonNull).distinct().toList();
            if (planIds.isEmpty()) {
                return Collections.emptyMap();
            }
            plans = productionPlanMapper.selectBatchIds(planIds);
        }
        return plans.stream().collect(Collectors.toMap(ProductionPlan::getPlanId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ProcessStep> fetchStepMap(List<PlanStep> planSteps) {
        List<Long> stepIds = planSteps.stream().map(PlanStep::getStepId).filter(Objects::nonNull).distinct().toList();
        if (stepIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return processStepMapper.selectBatchIds(stepIds).stream()
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
    }

    private Map<Long, Machine> fetchMachineMap(List<PlanStep> planSteps) {
        List<Long> machineIds = planSteps.stream().map(PlanStep::getMachineId).filter(Objects::nonNull).distinct().toList();
        if (machineIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return machineMapper.selectBatchIds(machineIds).stream()
                .collect(Collectors.toMap(Machine::getMachineId, Function.identity()));
    }

    private Map<Long, OrderInfo> fetchOrderMap(Iterable<ProductionPlan> plans) {
        List<Long> orderIds = streamPlans(plans)
                .map(ProductionPlan::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderInfoMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getOrderId, Function.identity()));
    }

    private Map<Long, BatchInfo> fetchBatchMap(Iterable<ProductionPlan> plans) {
        List<Long> batchIds = streamPlans(plans)
                .map(ProductionPlan::getBatchId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchInfoMapper.selectBatchIds(batchIds).stream()
                .collect(Collectors.toMap(BatchInfo::getBatchId, Function.identity()));
    }

    private java.util.stream.Stream<ProductionPlan> streamPlans(Iterable<ProductionPlan> plans) {
        return plans == null ? java.util.stream.Stream.empty() : java.util.stream.StreamSupport.stream(plans.spliterator(), false);
    }

    private Map<Long, QcRecord> buildLatestQcMap(List<PlanStep> planSteps) {
        List<Long> planStepIds = planSteps.stream().map(PlanStep::getPlanStepId).filter(Objects::nonNull).distinct().toList();
        if (planStepIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, QcRecord> latestMap = new LinkedHashMap<>();
        List<QcRecord> records = qcRecordMapper.selectList(Wrappers.<QcRecord>lambdaQuery()
                .in(QcRecord::getPlanStepId, planStepIds)
                .orderByDesc(QcRecord::getInspectTime)
                .orderByDesc(QcRecord::getInspectionId));
        for (QcRecord record : records) {
            latestMap.putIfAbsent(record.getPlanStepId(), record);
        }
        return latestMap;
    }

    private QcTaskVo toTaskVo(PlanStep step,
                              Map<Long, ProductionPlan> planMap,
                              Map<Long, ProcessStep> stepMap,
                              Map<Long, Machine> machineMap,
                              Map<Long, OrderInfo> orderMap,
                              Map<Long, BatchInfo> batchMap,
                              Map<Long, QcRecord> latestQcMap) {
        ProductionPlan plan = planMap.get(step.getPlanId());
        if (plan == null) {
            return null;
        }
        QcRecord latestQc = latestQcMap.get(step.getPlanStepId());
        ProcessStep processStep = stepMap.get(step.getStepId());
        Machine machine = machineMap.get(step.getMachineId());
        OrderInfo order = orderMap.get(plan.getOrderId());
        BatchInfo batch = batchMap.get(plan.getBatchId());

        QcTaskVo vo = new QcTaskVo();
        vo.setTaskNo("IQC-" + step.getPlanStepId());
        vo.setPlanStepId(step.getPlanStepId());
        vo.setPlanId(step.getPlanId());
        vo.setStepId(step.getStepId());
        vo.setStepName(processStep == null ? null : processStep.getStepName());
        vo.setMachineId(step.getMachineId());
        vo.setMachineName(machine == null ? null : machine.getMachineName());
        vo.setOrderId(plan.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setOrderItemId(plan.getOrderItemId());
        vo.setBatchId(plan.getBatchId());
        vo.setBatchNo(batch == null ? null : batch.getBatchNo());
        vo.setLatestInspectTime(latestQc == null ? null : latestQc.getInspectTime());
        vo.setLatestJudge(latestQc == null ? null : latestQc.getResultJudge());
        vo.setStatus(resolveTaskStatus(latestQc));
        return vo;
    }

    private String resolveTaskStatus(QcRecord latestQc) {
        if (latestQc == null || !StringUtils.hasText(latestQc.getResultJudge())) {
            return STATUS_PENDING;
        }
        if ("FAIL".equals(latestQc.getResultJudge()) || "RECHECK".equals(latestQc.getResultJudge())) {
            return STATUS_PENDING_HANDLE;
        }
        return STATUS_COMPLETED;
    }

    private boolean matchesKeyword(QcTaskVo task, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(task.getTaskNo(), keyword)
                || contains(String.valueOf(task.getPlanStepId()), keyword)
                || contains(task.getStepName(), keyword)
                || contains(task.getMachineName(), keyword)
                || contains(task.getOrderNo(), keyword)
                || contains(task.getBatchNo(), keyword);
    }

    private boolean matchesStatus(QcTaskVo task, String status) {
        return !StringUtils.hasText(status) || Objects.equals(task.getStatus(), status);
    }

    private boolean contains(String source, String keyword) {
        return StringUtils.hasText(source) && source.contains(keyword);
    }

    private List<QcTaskVo> paginate(List<QcTaskVo> rows, QcTaskQuery query) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        long fromIndex = Math.max(0L, (query.getPageNum() - 1) * query.getPageSize());
        if (fromIndex >= rows.size()) {
            return Collections.emptyList();
        }
        long toIndex = Math.min(rows.size(), fromIndex + query.getPageSize());
        return rows.subList((int) fromIndex, (int) toIndex);
    }
}
