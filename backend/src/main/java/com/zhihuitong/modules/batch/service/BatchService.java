package com.zhihuitong.modules.batch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.dto.BatchQuery;
import com.zhihuitong.modules.batch.dto.BatchUpsertRequest;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.mapper.BatchInfoMapper;
import com.zhihuitong.modules.batch.vo.BatchDetailVo;
import com.zhihuitong.modules.batch.vo.BatchImportResultVo;
import com.zhihuitong.modules.batch.vo.BatchListVo;
import com.zhihuitong.modules.batch.vo.BatchPlanSummaryVo;
import com.zhihuitong.modules.batch.vo.BatchQcSummaryVo;
import com.zhihuitong.modules.batch.vo.BatchResourcePoolVo;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.plan.entity.PlanStep;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BatchInfoMapper batchInfoMapper;
    private final ProductionPlanMapper productionPlanMapper;
    private final PlanStepMapper planStepMapper;
    private final QcRecordMapper qcRecordMapper;
    private final ExceptionRecordMapper exceptionRecordMapper;
    private final BatchResourcePoolService batchResourcePoolService;

    public BatchService(BatchInfoMapper batchInfoMapper,
                        ProductionPlanMapper productionPlanMapper,
                        PlanStepMapper planStepMapper,
                        QcRecordMapper qcRecordMapper,
                        ExceptionRecordMapper exceptionRecordMapper,
                        BatchResourcePoolService batchResourcePoolService) {
        this.batchInfoMapper = batchInfoMapper;
        this.productionPlanMapper = productionPlanMapper;
        this.planStepMapper = planStepMapper;
        this.qcRecordMapper = qcRecordMapper;
        this.exceptionRecordMapper = exceptionRecordMapper;
        this.batchResourcePoolService = batchResourcePoolService;
    }

    public TableDataInfo<BatchListVo> list(BatchQuery query) {
        validateDateRange(query.getDateFrom(), query.getDateTo(), "dateFrom must be earlier than or equal to dateTo");
        Page<BatchInfo> page = batchInfoMapper.selectPage(query.toPage(), buildQuery(query));
        List<BatchListVo> rows = page.getRecords().stream().map(this::toListVo).toList();
        Map<Long, String> statusMap = deriveBatchStatuses(rows.stream().map(BatchListVo::getBatchId).toList());
        rows.forEach(item -> item.setStatus(statusMap.getOrDefault(item.getBatchId(), "READY")));
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public BatchDetailVo getDetail(Long batchId) {
        BatchInfo batchInfo = requireBatch(batchId);
        BatchDetailVo detail = new BatchDetailVo();
        detail.setBatchId(batchInfo.getBatchId());
        detail.setBatchNo(batchInfo.getBatchNo());
        detail.setSupplier(batchInfo.getSupplier());
        detail.setInDate(batchInfo.getInDate());
        detail.setWeight(batchInfo.getWeight());
        detail.setWidth(batchInfo.getWidth());
        detail.setComposition(batchInfo.getComposition());
        detail.setNote(batchInfo.getNote());
        detail.setStatus(deriveBatchStatuses(List.of(batchId)).getOrDefault(batchId, "READY"));
        applyResourceFields(detail, batchResourcePoolService.getResource(batchId));

        List<ProductionPlan> plans = productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getBatchId, batchId)
                .orderByDesc(ProductionPlan::getCreateTime));
        detail.setPlanSummary(plans.stream().map(this::toPlanSummary).toList());

        List<Long> planIds = plans.stream().map(ProductionPlan::getPlanId).toList();
        if (planIds.isEmpty()) {
            detail.setQcSummary(Collections.emptyList());
            detail.setExceptionCount(0L);
            return detail;
        }

        List<PlanStep> planSteps = planStepMapper.selectList(Wrappers.<PlanStep>lambdaQuery()
                .in(PlanStep::getPlanId, planIds));
        List<Long> planStepIds = planSteps.stream().map(PlanStep::getPlanStepId).toList();
        if (planStepIds.isEmpty()) {
            detail.setQcSummary(Collections.emptyList());
            detail.setExceptionCount(0L);
            return detail;
        }

        List<QcRecord> qcRecords = qcRecordMapper.selectList(Wrappers.<QcRecord>lambdaQuery()
                .in(QcRecord::getPlanStepId, planStepIds)
                .orderByDesc(QcRecord::getInspectTime)
                .last("limit 10"));
        detail.setQcSummary(qcRecords.stream().map(this::toQcSummary).toList());

        Long exceptionCount = exceptionRecordMapper.selectCount(Wrappers.<ExceptionRecord>lambdaQuery()
                .in(ExceptionRecord::getPlanStepId, planStepIds));
        detail.setExceptionCount(exceptionCount == null ? 0L : exceptionCount);
        return detail;
    }

    @Transactional
    public Map<String, Object> create(BatchUpsertRequest request) {
        checkBatchNoUnique(request.getBatchNo(), null);
        BatchInfo entity = new BatchInfo();
        copyUpsertRequest(request, entity);
        batchInfoMapper.insert(entity);
        return Map.of(
                "batchId", entity.getBatchId(),
                "batchNo", entity.getBatchNo()
        );
    }

    @Transactional
    public Map<String, Object> update(Long batchId, BatchUpsertRequest request) {
        BatchInfo entity = requireBatch(batchId);
        checkBatchNoUnique(request.getBatchNo(), batchId);
        copyUpsertRequest(request, entity);
        batchInfoMapper.updateById(entity);
        return Map.of(
                "batchId", entity.getBatchId(),
                "batchNo", entity.getBatchNo()
        );
    }

    @Transactional
    public void delete(Long batchId) {
        requireBatch(batchId);
        Long refCount = productionPlanMapper.selectCount(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getBatchId, batchId));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(409, "Batch is already referenced by production plans and cannot be deleted");
        }
        batchInfoMapper.deleteById(batchId);
    }

    @Transactional
    public BatchImportResultVo importBatches(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "Import file must not be empty");
        }
        List<String> lines = new String(file.getBytes(), StandardCharsets.UTF_8).lines().toList();
        BatchImportResultVo result = new BatchImportResultVo();
        result.setTotalCount(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (index == 0 && line.toLowerCase().contains("batchno")) {
                result.setTotalCount(result.getTotalCount() - 1);
                continue;
            }
            try {
                BatchUpsertRequest request = parseCsvLine(line);
                create(request);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception exception) {
                result.setFailCount(result.getFailCount() + 1);
                result.getErrors().add("line " + (index + 1) + ": " + exception.getMessage());
            }
        }
        return result;
    }

    public void exportBatches(HttpServletResponse response) throws IOException {
        List<BatchInfo> batches = batchInfoMapper.selectList(Wrappers.<BatchInfo>lambdaQuery()
                .orderByDesc(BatchInfo::getInDate));
        StringBuilder builder = new StringBuilder();
        builder.append("batchNo,supplier,inDate,weight,width,composition,note").append(System.lineSeparator());
        for (BatchInfo batch : batches) {
            builder.append(csv(batch.getBatchNo())).append(',')
                    .append(csv(batch.getSupplier())).append(',')
                    .append(csv(formatTime(batch.getInDate()))).append(',')
                    .append(csv(batch.getWeight())).append(',')
                    .append(csv(batch.getWidth())).append(',')
                    .append(csv(batch.getComposition())).append(',')
                    .append(csv(batch.getNote()))
                    .append(System.lineSeparator());
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("batches.csv", StandardCharsets.UTF_8));
        response.getWriter().write(builder.toString());
        response.getWriter().flush();
    }

    public BatchInfo requireBatch(Long batchId) {
        BatchInfo batchInfo = batchInfoMapper.selectById(batchId);
        if (batchInfo == null) {
            throw new BusinessException(404, "批次不存在");
        }
        return batchInfo;
    }

    private LambdaQueryWrapper<BatchInfo> buildQuery(BatchQuery query) {
        return Wrappers.<BatchInfo>lambdaQuery()
                .like(StringUtils.hasText(query.getBatchNo()), BatchInfo::getBatchNo, query.getBatchNo())
                .like(StringUtils.hasText(query.getSupplier()), BatchInfo::getSupplier, query.getSupplier())
                .ge(query.getDateFrom() != null, BatchInfo::getInDate, query.getDateFrom())
                .le(query.getDateTo() != null, BatchInfo::getInDate, query.getDateTo())
                .orderByDesc(BatchInfo::getInDate);
    }

    private BatchListVo toListVo(BatchInfo batchInfo) {
        BatchListVo vo = new BatchListVo();
        vo.setBatchId(batchInfo.getBatchId());
        vo.setBatchNo(batchInfo.getBatchNo());
        vo.setSupplier(batchInfo.getSupplier());
        vo.setInDate(batchInfo.getInDate());
        vo.setWeight(batchInfo.getWeight());
        vo.setWidth(batchInfo.getWidth());
        vo.setComposition(batchInfo.getComposition());
        vo.setNote(batchInfo.getNote());
        return vo;
    }

    private BatchPlanSummaryVo toPlanSummary(ProductionPlan plan) {
        BatchPlanSummaryVo vo = new BatchPlanSummaryVo();
        vo.setPlanId(plan.getPlanId());
        vo.setStatus(plan.getStatus());
        vo.setPlanStartTime(plan.getPlanStartTime());
        vo.setPlanEndTime(plan.getPlanEndTime());
        return vo;
    }

    private BatchQcSummaryVo toQcSummary(QcRecord record) {
        BatchQcSummaryVo vo = new BatchQcSummaryVo();
        vo.setInspectionId(record.getInspectionId());
        vo.setPlanStepId(record.getPlanStepId());
        vo.setInspectType(record.getInspectType());
        vo.setInspectTime(record.getInspectTime());
        vo.setResultJudge(record.getResultJudge());
        return vo;
    }

    private void applyResourceFields(BatchDetailVo detail, BatchResourcePoolVo resource) {
        detail.setResourceStatus(resource.getResourceStatus());
        detail.setResourceStatusLabel(resource.getResourceStatusLabel());
        detail.setLinkedOrderCount(resource.getLinkedOrderCount());
        detail.setLinkedOrders(resource.getLinkedOrders());
        detail.setAllocatedWeight(resource.getAllocatedWeight());
        detail.setAllocatedQuantity(resource.getAllocatedQuantity());
        detail.setRemainingWeight(resource.getRemainingWeight());
        detail.setRemainingQuantity(resource.getRemainingQuantity());
        detail.setCurrentPlanId(resource.getCurrentPlanId());
        detail.setCurrentPlanStatus(resource.getCurrentPlanStatus());
        detail.setCurrentOrderId(resource.getCurrentOrderId());
        detail.setCurrentOrderNo(resource.getCurrentOrderNo());
        detail.setLockedByPlan(resource.isLockedByPlan());
        detail.setReadyForSchedule(resource.isReadyForSchedule());
    }

    private void copyUpsertRequest(BatchUpsertRequest request, BatchInfo entity) {
        entity.setBatchNo(request.getBatchNo().trim());
        entity.setSupplier(request.getSupplier().trim());
        entity.setInDate(request.getInDate());
        entity.setWeight(request.getWeight());
        entity.setWidth(request.getWidth());
        entity.setComposition(request.getComposition());
        entity.setNote(request.getNote());
    }

    private void checkBatchNoUnique(String batchNo, Long excludeId) {
        Long count = batchInfoMapper.selectCount(Wrappers.<BatchInfo>lambdaQuery()
                .eq(BatchInfo::getBatchNo, batchNo)
                .ne(excludeId != null, BatchInfo::getBatchId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "Batch number already exists");
        }
    }

    private Map<Long, String> deriveBatchStatuses(List<Long> batchIds) {
        if (batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductionPlan> plans = productionPlanMapper.selectList(Wrappers.<ProductionPlan>lambdaQuery()
                .in(ProductionPlan::getBatchId, batchIds));
        Map<Long, List<ProductionPlan>> grouped = plans.stream().collect(Collectors.groupingBy(ProductionPlan::getBatchId));
        Map<Long, String> statusMap = new LinkedHashMap<>();
        for (Long batchId : batchIds) {
            List<ProductionPlan> planList = grouped.getOrDefault(batchId, Collections.emptyList());
            statusMap.put(batchId, deriveBatchStatus(planList));
        }
        return statusMap;
    }

    private String deriveBatchStatus(List<ProductionPlan> planList) {
        if (planList.isEmpty()) {
            // The schema has no batch status column, so we infer READY for newly received batches.
            return "READY";
        }
        Set<String> statuses = planList.stream()
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

    private BatchUpsertRequest parseCsvLine(String line) {
        String[] segments = line.split(",", -1);
        if (segments.length < 3) {
            throw new BusinessException(400, "CSV line must contain at least batchNo,supplier,inDate");
        }
        BatchUpsertRequest request = new BatchUpsertRequest();
        request.setBatchNo(segments[0].trim());
        request.setSupplier(segments[1].trim());
        request.setInDate(parseDateTime(segments[2].trim()));
        if (segments.length > 3 && StringUtils.hasText(segments[3])) {
            request.setWeight(new BigDecimal(segments[3].trim()));
        }
        if (segments.length > 4 && StringUtils.hasText(segments[4])) {
            request.setWidth(new BigDecimal(segments[4].trim()));
        }
        if (segments.length > 5) {
            request.setComposition(segments[5].trim());
        }
        if (segments.length > 6) {
            request.setNote(segments[6].trim());
        }
        return request;
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(400, "Invalid datetime format: " + value);
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, message);
        }
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
