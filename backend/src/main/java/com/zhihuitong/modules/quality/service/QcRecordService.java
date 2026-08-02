package com.zhihuitong.modules.quality.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.dto.QcRecordCloseRequest;
import com.zhihuitong.modules.quality.dto.QcRecordQuery;
import com.zhihuitong.modules.quality.dto.QcRecordReviewRequest;
import com.zhihuitong.modules.quality.dto.QcRecordUpsertRequest;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import com.zhihuitong.security.service.DataScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class QcRecordService {

    private static final Set<String> INSPECT_TYPES = Set.of("offline", "video");
    private static final Set<String> RESULT_JUDGES = Set.of("PASS", "FAIL", "RECHECK");

    private final QcRecordMapper qcRecordMapper;
    private final QcItemService qcItemService;
    private final PlanStepService planStepService;
    private final QcCameraService qcCameraService;
    private final ExceptionRecordMapper exceptionRecordMapper;
    private final DataScopeService dataScopeService;

    public QcRecordService(QcRecordMapper qcRecordMapper,
                           QcItemService qcItemService,
                           PlanStepService planStepService,
                           QcCameraService qcCameraService,
                           ExceptionRecordMapper exceptionRecordMapper,
                           DataScopeService dataScopeService) {
        this.qcRecordMapper = qcRecordMapper;
        this.qcItemService = qcItemService;
        this.planStepService = planStepService;
        this.qcCameraService = qcCameraService;
        this.exceptionRecordMapper = exceptionRecordMapper;
        this.dataScopeService = dataScopeService;
    }

    public TableDataInfo<QcRecord> list(QcRecordQuery query) {
        validateTimeRange(query.getInspectTimeFrom(), query.getInspectTimeTo(), "inspectTimeFrom must be earlier than or equal to inspectTimeTo");
        Page<QcRecord> page = qcRecordMapper.selectPage(query.toPage(), dataScopeService.apply(Wrappers.<QcRecord>lambdaQuery(), QcRecord::getDeptId, QcRecord::getCreatedBy)
                .eq(query.getPlanStepId() != null, QcRecord::getPlanStepId, query.getPlanStepId())
                .eq(query.getQcItemId() != null, QcRecord::getQcItemId, query.getQcItemId())
                .eq(StringUtils.hasText(query.getInspectType()), QcRecord::getInspectType, query.getInspectType())
                .eq(StringUtils.hasText(query.getResultJudge()), QcRecord::getResultJudge, query.getResultJudge())
                .ge(query.getInspectTimeFrom() != null, QcRecord::getInspectTime, query.getInspectTimeFrom())
                .le(query.getInspectTimeTo() != null, QcRecord::getInspectTime, query.getInspectTimeTo())
                .orderByDesc(QcRecord::getInspectTime));
        return TableDataInfoBuilder.build(page);
    }

    public QcRecord getDetail(Long inspectionId) {
        return requireRecord(inspectionId);
    }

    @Transactional
    public QcRecord create(QcRecordUpsertRequest request) {
        validateRequest(request);
        QcRecord entity = new QcRecord();
        copyRequest(request, entity);
        entity.setDeptId(dataScopeService.currentDeptId());
        entity.setCreatedBy(dataScopeService.currentUserId());
        qcRecordMapper.insert(entity);
        autoCreateExceptionIfNeeded(entity);
        return entity;
    }

    @Transactional
    public QcRecord update(Long inspectionId, QcRecordUpsertRequest request) {
        validateRequest(request);
        QcRecord entity = requireRecord(inspectionId);
        copyRequest(request, entity);
        qcRecordMapper.updateById(entity);
        autoCreateExceptionIfNeeded(entity);
        return entity;
    }

    @Transactional
    public QcRecord review(Long inspectionId, QcRecordReviewRequest request) {
        assertResultJudge(request.getReviewResult());
        QcRecord entity = requireRecord(inspectionId);
        entity.setResultJudge(request.getReviewResult());
        String reviewer = StringUtils.hasText(request.getReviewer()) ? request.getReviewer() : "unknown";
        String reviewText = "reviewer=" + reviewer + ", remark=" + request.getReviewRemark();
        // The current schema has no dedicated review fields, so review audit is persisted into remark.
        entity.setRemark(AuditRemarkUtils.append(entity.getRemark(), "QC_REVIEW", reviewText));
        qcRecordMapper.updateById(entity);
        autoCreateExceptionIfNeeded(entity);
        return entity;
    }

    @Transactional
    public QcRecord close(Long inspectionId, QcRecordCloseRequest request) {
        QcRecord entity = requireRecord(inspectionId);
        // The schema has no close-state column, so we preserve close actions as audit remarks.
        entity.setRemark(AuditRemarkUtils.append(entity.getRemark(), "QC_CLOSE", request.getCloseRemark()));
        qcRecordMapper.updateById(entity);
        return entity;
    }

    public QcRecord requireRecord(Long inspectionId) {
        QcRecord entity = qcRecordMapper.selectById(inspectionId);
        if (entity == null) {
            throw new BusinessException(404, "质检记录不存在");
        }
        return entity;
    }

    private void validateRequest(QcRecordUpsertRequest request) {
        planStepService.requirePlanStep(request.getPlanStepId());
        qcItemService.requireQcItem(request.getQcItemId());
        if (!INSPECT_TYPES.contains(request.getInspectType())) {
            throw new BusinessException(422, "inspectType 仅支持 offline 或 video");
        }
        assertResultJudge(request.getResultJudge());
        if ("video".equals(request.getInspectType()) && request.getCameraId() != null) {
            qcCameraService.requireCamera(request.getCameraId());
        }
        if ("video".equals(request.getInspectType()) && request.getCameraId() == null) {
            throw new BusinessException(422, "视频质检必须传入 cameraId");
        }
        if (request.getCameraId() != null) {
            qcCameraService.requireCamera(request.getCameraId());
        }
    }

    private void copyRequest(QcRecordUpsertRequest request, QcRecord entity) {
        entity.setPlanStepId(request.getPlanStepId());
        entity.setQcItemId(request.getQcItemId());
        entity.setInspectTime(request.getInspectTime());
        entity.setInspectType(request.getInspectType());
        entity.setCameraId(request.getCameraId());
        entity.setFrameTime(request.getFrameTime());
        entity.setImageUrl(request.getImageUrl());
        entity.setConfidenceScore(request.getConfidenceScore());
        entity.setResultValue(request.getResultValue());
        entity.setResultJudge(request.getResultJudge());
        entity.setInspector(request.getInspector());
        entity.setRemark(request.getRemark());
    }

    private void assertResultJudge(String resultJudge) {
        if (!RESULT_JUDGES.contains(resultJudge)) {
            throw new BusinessException(422, "不支持的质检结论: " + resultJudge);
        }
    }

    private void autoCreateExceptionIfNeeded(QcRecord entity) {
        if (!"FAIL".equals(entity.getResultJudge())) {
            return;
        }
        Long count = exceptionRecordMapper.selectCount(Wrappers.<ExceptionRecord>lambdaQuery()
                .eq(ExceptionRecord::getPlanStepId, entity.getPlanStepId())
                .eq(ExceptionRecord::getExceptionType, "QUALITY")
                .eq(ExceptionRecord::getStatus, "OPEN"));
        if (count != null && count > 0) {
            return;
        }
        ExceptionRecord exceptionRecord = new ExceptionRecord();
        exceptionRecord.setPlanStepId(entity.getPlanStepId());
        exceptionRecord.setExceptionType("QUALITY");
        exceptionRecord.setExceptionLevel("MEDIUM");
        exceptionRecord.setDescription("Auto-created from QC failure record " + entity.getInspectionId());
        exceptionRecord.setHandleResult("");
        exceptionRecord.setCreateTime(LocalDateTime.now());
        exceptionRecord.setStatus("OPEN");
        exceptionRecord.setDeptId(entity.getDeptId());
        exceptionRecord.setCreatedBy(entity.getCreatedBy());
        exceptionRecordMapper.insert(exceptionRecord);
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }
}
