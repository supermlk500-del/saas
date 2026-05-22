package com.zhihuitong.modules.exception.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.exception.dto.ExceptionCloseRequest;
import com.zhihuitong.modules.exception.dto.ExceptionRecordQuery;
import com.zhihuitong.modules.exception.dto.ExceptionRecordUpsertRequest;
import com.zhihuitong.modules.exception.dto.ExceptionReworkRequest;
import com.zhihuitong.modules.exception.dto.ExceptionStatusPatchRequest;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.plan.service.PlanStepService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
public class ExceptionRecordService {

    private static final Set<String> EXCEPTION_STATUSES = Set.of("OPEN", "PROCESSING", "CLOSED");
    private static final Set<String> EXCEPTION_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final ExceptionRecordMapper exceptionRecordMapper;
    private final PlanStepService planStepService;

    public ExceptionRecordService(ExceptionRecordMapper exceptionRecordMapper,
                                  PlanStepService planStepService) {
        this.exceptionRecordMapper = exceptionRecordMapper;
        this.planStepService = planStepService;
    }

    public TableDataInfo<ExceptionRecord> list(ExceptionRecordQuery query) {
        Page<ExceptionRecord> page = exceptionRecordMapper.selectPage(query.toPage(), Wrappers.<ExceptionRecord>lambdaQuery()
                .eq(query.getPlanStepId() != null, ExceptionRecord::getPlanStepId, query.getPlanStepId())
                .eq(StringUtils.hasText(query.getExceptionType()), ExceptionRecord::getExceptionType, query.getExceptionType())
                .eq(StringUtils.hasText(query.getExceptionLevel()), ExceptionRecord::getExceptionLevel, query.getExceptionLevel())
                .eq(StringUtils.hasText(query.getStatus()), ExceptionRecord::getStatus, query.getStatus())
                .orderByDesc(ExceptionRecord::getCreateTime));
        return TableDataInfoBuilder.build(page);
    }

    public ExceptionRecord getDetail(Long exceptionId) {
        return requireRecord(exceptionId);
    }

    @Transactional
    public ExceptionRecord create(ExceptionRecordUpsertRequest request) {
        validateRequest(request);
        ExceptionRecord entity = new ExceptionRecord();
        copyRequest(request, entity);
        exceptionRecordMapper.insert(entity);
        return entity;
    }

    @Transactional
    public ExceptionRecord update(Long exceptionId, ExceptionRecordUpsertRequest request) {
        validateRequest(request);
        ExceptionRecord entity = requireRecord(exceptionId);
        copyRequest(request, entity);
        exceptionRecordMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public ExceptionRecord patchStatus(Long exceptionId, ExceptionStatusPatchRequest request) {
        assertStatus(request.getStatus());
        ExceptionRecord entity = requireRecord(exceptionId);
        entity.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getRemark())) {
            entity.setHandleResult(AuditRemarkUtils.append(entity.getHandleResult(), "STATUS_CHANGE", request.getRemark()));
        }
        exceptionRecordMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public ExceptionRecord close(Long exceptionId, ExceptionCloseRequest request) {
        ExceptionRecord entity = requireRecord(exceptionId);
        entity.setStatus("CLOSED");
        entity.setHandleResult(request.getHandleResult());
        if (StringUtils.hasText(request.getCloseRemark())) {
            entity.setHandleResult(AuditRemarkUtils.append(entity.getHandleResult(), "CLOSE", request.getCloseRemark()));
        }
        exceptionRecordMapper.updateById(entity);
        return entity;
    }

    @Transactional
    public ExceptionRecord rework(Long exceptionId, ExceptionReworkRequest request) {
        ExceptionRecord entity = requireRecord(exceptionId);
        entity.setStatus("PROCESSING");
        String reworkText = "plan=" + request.getReworkPlan()
                + ", owner=" + request.getReworkOwner()
                + ", expectedFinishTime=" + request.getExpectedFinishTime();
        entity.setHandleResult(AuditRemarkUtils.append(entity.getHandleResult(), "REWORK", reworkText));
        exceptionRecordMapper.updateById(entity);
        return entity;
    }

    public ExceptionRecord requireRecord(Long exceptionId) {
        ExceptionRecord entity = exceptionRecordMapper.selectById(exceptionId);
        if (entity == null) {
            throw new BusinessException(404, "Exception record not found");
        }
        return entity;
    }

    private void validateRequest(ExceptionRecordUpsertRequest request) {
        planStepService.requirePlanStep(request.getPlanStepId());
        assertStatus(request.getStatus());
        if (!EXCEPTION_LEVELS.contains(request.getExceptionLevel())) {
            throw new BusinessException(422, "Unsupported exceptionLevel: " + request.getExceptionLevel());
        }
    }

    private void copyRequest(ExceptionRecordUpsertRequest request, ExceptionRecord entity) {
        entity.setPlanStepId(request.getPlanStepId());
        entity.setExceptionType(request.getExceptionType().trim());
        entity.setExceptionLevel(request.getExceptionLevel().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setHandleResult(request.getHandleResult());
        entity.setCreateTime(request.getCreateTime());
        entity.setStatus(request.getStatus().trim());
    }

    private void assertStatus(String status) {
        if (!EXCEPTION_STATUSES.contains(status)) {
            throw new BusinessException(422, "Unsupported exception status: " + status);
        }
    }
}
