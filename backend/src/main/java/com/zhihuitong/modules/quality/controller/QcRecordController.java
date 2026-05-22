package com.zhihuitong.modules.quality.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.quality.dto.QcRecordCloseRequest;
import com.zhihuitong.modules.quality.dto.QcRecordQuery;
import com.zhihuitong.modules.quality.dto.QcRecordReviewRequest;
import com.zhihuitong.modules.quality.dto.QcRecordUpsertRequest;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.service.QcRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/qc-records")
public class QcRecordController {

    private final QcRecordService qcRecordService;

    public QcRecordController(QcRecordService qcRecordService) {
        this.qcRecordService = qcRecordService;
    }

    @GetMapping
    public TableDataInfo<QcRecord> list(@Valid @ModelAttribute QcRecordQuery query) {
        return qcRecordService.list(query);
    }

    @GetMapping("/{inspectionId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "inspectionId must be greater than 0") Long inspectionId) {
        return AjaxResult.success(qcRecordService.getDetail(inspectionId));
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody QcRecordUpsertRequest request) {
        return AjaxResult.success(qcRecordService.create(request));
    }

    @PutMapping("/{inspectionId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "inspectionId must be greater than 0") Long inspectionId,
                             @Valid @RequestBody QcRecordUpsertRequest request) {
        return AjaxResult.success(qcRecordService.update(inspectionId, request));
    }

    @PatchMapping("/{inspectionId}/review")
    public AjaxResult review(@PathVariable @Min(value = 1, message = "inspectionId must be greater than 0") Long inspectionId,
                             @Valid @RequestBody QcRecordReviewRequest request) {
        return AjaxResult.success(qcRecordService.review(inspectionId, request));
    }

    @PatchMapping("/{inspectionId}/close")
    public AjaxResult close(@PathVariable @Min(value = 1, message = "inspectionId must be greater than 0") Long inspectionId,
                            @Valid @RequestBody QcRecordCloseRequest request) {
        return AjaxResult.success(qcRecordService.close(inspectionId, request));
    }
}
