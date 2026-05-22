package com.zhihuitong.modules.process.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.process.dto.IsActivePatchRequest;
import com.zhihuitong.modules.process.dto.ProcessStepQuery;
import com.zhihuitong.modules.process.dto.ProcessStepUpsertRequest;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.mapper.ProcessStepMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcessStepService {

    private final ProcessStepMapper processStepMapper;

    public ProcessStepService(ProcessStepMapper processStepMapper) {
        this.processStepMapper = processStepMapper;
    }

    public TableDataInfo<ProcessStep> list(ProcessStepQuery query) {
        Page<ProcessStep> page = processStepMapper.selectPage(query.toPage(), Wrappers.<ProcessStep>lambdaQuery()
                .like(StringUtils.hasText(query.getStepCode()), ProcessStep::getStepCode, query.getStepCode())
                .like(StringUtils.hasText(query.getStepName()), ProcessStep::getStepName, query.getStepName())
                .eq(StringUtils.hasText(query.getStepType()), ProcessStep::getStepType, query.getStepType())
                .eq(query.getIsActive() != null, ProcessStep::getIsActive, query.getIsActive())
                .orderByAsc(ProcessStep::getSortOrder)
                .orderByAsc(ProcessStep::getStepId));
        return TableDataInfoBuilder.build(page);
    }

    public ProcessStep getDetail(Long stepId) {
        return requireStep(stepId);
    }

    @Transactional
    public ProcessStep create(ProcessStepUpsertRequest request) {
        checkStepCodeUnique(request.getStepCode(), null);
        ProcessStep entity = new ProcessStep();
        copyRequest(request, entity);
        processStepMapper.insert(entity);
        return entity;
    }

    @Transactional
    public ProcessStep update(Long stepId, ProcessStepUpsertRequest request) {
        ProcessStep entity = requireStep(stepId);
        checkStepCodeUnique(request.getStepCode(), stepId);
        copyRequest(request, entity);
        processStepMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public ProcessStep patchStatus(Long stepId, IsActivePatchRequest request) {
        ProcessStep entity = requireStep(stepId);
        entity.setIsActive(request.getIsActive());
        processStepMapper.updateById(entity);
        return entity;
    }

    public ProcessStep requireStep(Long stepId) {
        ProcessStep entity = processStepMapper.selectById(stepId);
        if (entity == null) {
            throw new BusinessException(404, "Process step not found");
        }
        return entity;
    }

    private void copyRequest(ProcessStepUpsertRequest request, ProcessStep entity) {
        entity.setStepCode(request.getStepCode().trim());
        entity.setStepName(request.getStepName().trim());
        entity.setStepType(request.getStepType());
        entity.setSortOrder(request.getSortOrder());
        entity.setDefaultHours(request.getDefaultHours());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive());
    }

    private void checkStepCodeUnique(String stepCode, Long excludeId) {
        Long count = processStepMapper.selectCount(Wrappers.<ProcessStep>lambdaQuery()
                .eq(ProcessStep::getStepCode, stepCode)
                .ne(excludeId != null, ProcessStep::getStepId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "Process step code already exists");
        }
    }
}
