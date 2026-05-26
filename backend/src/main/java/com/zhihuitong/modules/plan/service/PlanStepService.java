package com.zhihuitong.modules.plan.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.plan.dto.PlanStepMachinePatchRequest;
import com.zhihuitong.modules.plan.dto.PlanStepQuery;
import com.zhihuitong.modules.plan.dto.PlanStepUpdateRequest;
import com.zhihuitong.modules.plan.dto.StatusPatchRequest;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.vo.PlanStepVo;
import com.zhihuitong.modules.process.service.MachineService;
import com.zhihuitong.modules.process.service.StepMachineCapabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PlanStepService {

    private static final Set<String> STEP_STATUSES = Set.of("PENDING", "READY", "RUNNING", "PAUSED", "FINISHED", "ABNORMAL");

    private final PlanStepMapper planStepMapper;
    private final ProductionPlanService productionPlanService;
    private final MachineService machineService;
    private final StepMachineCapabilityService capabilityService;
    private final BatchService batchService;

    public PlanStepService(PlanStepMapper planStepMapper,
                           ProductionPlanService productionPlanService,
                           MachineService machineService,
                           StepMachineCapabilityService capabilityService,
                           BatchService batchService) {
        this.planStepMapper = planStepMapper;
        this.productionPlanService = productionPlanService;
        this.machineService = machineService;
        this.capabilityService = capabilityService;
        this.batchService = batchService;
    }

    public TableDataInfo<PlanStepVo> list(PlanStepQuery query) {
        validateTimeRange(query.getDateFrom(), query.getDateTo(), "dateFrom must be earlier than or equal to dateTo");
        Page<PlanStep> page = planStepMapper.selectPage(query.toPage(), Wrappers.<PlanStep>lambdaQuery()
                .eq(query.getPlanId() != null, PlanStep::getPlanId, query.getPlanId())
                .eq(query.getStepId() != null, PlanStep::getStepId, query.getStepId())
                .eq(query.getMachineId() != null, PlanStep::getMachineId, query.getMachineId())
                .eq(StringUtils.hasText(query.getStatus()), PlanStep::getStatus, query.getStatus())
                .ge(query.getDateFrom() != null, PlanStep::getPlanStartTime, query.getDateFrom())
                .le(query.getDateTo() != null, PlanStep::getPlanStartTime, query.getDateTo())
                .orderByAsc(PlanStep::getSequenceNo));
        List<PlanStepVo> rows = productionPlanService.buildPlanStepVos(page.getRecords());
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public PlanStepVo getDetail(Long planStepId) {
        return productionPlanService.buildPlanStepVos(List.of(requirePlanStep(planStepId))).get(0);
    }

    @Transactional
    public PlanStepVo update(Long planStepId, PlanStepUpdateRequest request) {
        PlanStep step = requirePlanStep(planStepId);
        validateTimeRange(request.getPlanStartTime(), request.getPlanEndTime(), "planStartTime must be earlier than or equal to planEndTime");
        if (request.getPlanStartTime() != null) {
            step.setPlanStartTime(request.getPlanStartTime());
        }
        if (request.getPlanEndTime() != null) {
            step.setPlanEndTime(request.getPlanEndTime());
        }
        if (request.getPlanHours() != null) {
            step.setPlanHours(request.getPlanHours());
        }
        if (request.getSequenceNo() != null) {
            step.setSequenceNo(request.getSequenceNo());
        }
        if (StringUtils.hasText(request.getStatus())) {
            assertStepStatus(request.getStatus());
            step.setStatus(request.getStatus());
        }
        if (request.getRemark() != null) {
            step.setRemark(request.getRemark());
        }
        planStepMapper.updateById(step);
        syncPlanWindow(step.getPlanId());
        return getDetail(planStepId);
    }

    @Transactional
    public PlanStepVo patchMachine(Long planStepId, PlanStepMachinePatchRequest request) {
        PlanStep step = requirePlanStep(planStepId);
        machineService.requireMachine(request.getMachineId());
        ProductionPlan plan = productionPlanService.requirePlan(step.getPlanId());
        BatchInfo batch = batchService.requireBatch(plan.getBatchId());
        if (!capabilityService.supportsBatch(step.getStepId(), request.getMachineId(), batch.getWidth(), batch.getWeight())) {
            throw new BusinessException(422, "Target machine is not supported by active capability constraints for this batch");
        }
        step.setMachineId(request.getMachineId());
        planStepMapper.updateById(step);
        return getDetail(planStepId);
    }

    @Transactional
    public PlanStepVo patchStatus(Long planStepId, StatusPatchRequest request) {
        assertStepStatus(request.getStatus());
        PlanStep step = requirePlanStep(planStepId);
        step.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getReason())) {
            step.setRemark(AuditRemarkUtils.append(step.getRemark(), "STEP_STATUS", request.getReason()));
        }
        planStepMapper.updateById(step);
        return getDetail(planStepId);
    }

    public PlanStep requirePlanStep(Long planStepId) {
        PlanStep step = planStepMapper.selectById(planStepId);
        if (step == null) {
            throw new BusinessException(404, "计划工序不存在");
        }
        return step;
    }

    private void assertStepStatus(String status) {
        if (!STEP_STATUSES.contains(status)) {
            throw new BusinessException(422, "Unsupported plan step status: " + status);
        }
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }

    private void syncPlanWindow(Long planId) {
        List<PlanStep> steps = productionPlanService.findPlanSteps(planId);
        if (steps.isEmpty()) {
            return;
        }
        ProductionPlan plan = productionPlanService.requirePlan(planId);
        plan.setPlanStartTime(steps.get(0).getPlanStartTime());
        plan.setPlanEndTime(steps.get(steps.size() - 1).getPlanEndTime());
        productionPlanService.update(planId, toUpdateRequest(plan));
    }

    private com.zhihuitong.modules.plan.dto.ProductionPlanUpdateRequest toUpdateRequest(ProductionPlan plan) {
        com.zhihuitong.modules.plan.dto.ProductionPlanUpdateRequest request = new com.zhihuitong.modules.plan.dto.ProductionPlanUpdateRequest();
        request.setPlanStartTime(plan.getPlanStartTime());
        request.setPlanEndTime(plan.getPlanEndTime());
        request.setStatus(plan.getStatus());
        request.setRemark(plan.getRemark());
        return request;
    }
}
