package com.zhihuitong.modules.plan.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.plan.dto.ProcessParameterQuery;
import com.zhihuitong.modules.plan.dto.ProcessParameterUpsertRequest;
import com.zhihuitong.modules.plan.entity.ProcessParameter;
import com.zhihuitong.modules.plan.mapper.ProcessParameterMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ProcessParameterService {

    private final ProcessParameterMapper processParameterMapper;
    private final PlanStepService planStepService;

    public ProcessParameterService(ProcessParameterMapper processParameterMapper,
                                   PlanStepService planStepService) {
        this.processParameterMapper = processParameterMapper;
        this.planStepService = planStepService;
    }

    public TableDataInfo<ProcessParameter> list(ProcessParameterQuery query) {
        validateTimeRange(query.getRecordTimeFrom(), query.getRecordTimeTo(), "recordTimeFrom must be earlier than or equal to recordTimeTo");
        Page<ProcessParameter> page = processParameterMapper.selectPage(query.toPage(), Wrappers.<ProcessParameter>lambdaQuery()
                .eq(query.getPlanStepId() != null, ProcessParameter::getPlanStepId, query.getPlanStepId())
                .eq(StringUtils.hasText(query.getParamType()), ProcessParameter::getParamType, query.getParamType())
                .ge(query.getRecordTimeFrom() != null, ProcessParameter::getRecordTime, query.getRecordTimeFrom())
                .le(query.getRecordTimeTo() != null, ProcessParameter::getRecordTime, query.getRecordTimeTo())
                .orderByDesc(ProcessParameter::getRecordTime));
        return TableDataInfoBuilder.build(page);
    }

    public ProcessParameter getDetail(Long paramId) {
        return requireParameter(paramId);
    }

    @Transactional
    public ProcessParameter create(ProcessParameterUpsertRequest request) {
        planStepService.requirePlanStep(request.getPlanStepId());
        ProcessParameter entity = new ProcessParameter();
        copyRequest(request, entity);
        processParameterMapper.insert(entity);
        return entity;
    }

    @Transactional
    public ProcessParameter update(Long paramId, ProcessParameterUpsertRequest request) {
        planStepService.requirePlanStep(request.getPlanStepId());
        ProcessParameter entity = requireParameter(paramId);
        copyRequest(request, entity);
        processParameterMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public void delete(Long paramId) {
        requireParameter(paramId);
        processParameterMapper.deleteById(paramId);
    }

    public ProcessParameter requireParameter(Long paramId) {
        ProcessParameter entity = processParameterMapper.selectById(paramId);
        if (entity == null) {
            throw new BusinessException(404, "Process parameter not found");
        }
        return entity;
    }

    private void copyRequest(ProcessParameterUpsertRequest request, ProcessParameter entity) {
        entity.setPlanStepId(request.getPlanStepId());
        entity.setParamName(request.getParamName().trim());
        entity.setParamValue(request.getParamValue().trim());
        entity.setUnit(request.getUnit());
        entity.setParamType(request.getParamType());
        entity.setRecordTime(request.getRecordTime());
        entity.setRemark(request.getRemark());
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }
}
