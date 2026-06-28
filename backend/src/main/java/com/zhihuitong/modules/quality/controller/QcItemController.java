package com.zhihuitong.modules.quality.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.quality.dto.QcItemQuery;
import com.zhihuitong.modules.quality.dto.QcItemStatusPatchRequest;
import com.zhihuitong.modules.quality.dto.QcItemUpsertRequest;
import com.zhihuitong.modules.quality.entity.QcItem;
import com.zhihuitong.modules.quality.service.QcItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("@auth.hasPermission('quality:realtime:view')")
@RequestMapping("/api/qc-items")
public class QcItemController {

    private final QcItemService qcItemService;

    public QcItemController(QcItemService qcItemService) {
        this.qcItemService = qcItemService;
    }

    @GetMapping
    public TableDataInfo<QcItem> list(@Valid @ModelAttribute QcItemQuery query) {
        return qcItemService.list(query);
    }

    @GetMapping("/{qcItemId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "qcItemId must be greater than 0") Long qcItemId) {
        return AjaxResult.success(qcItemService.getDetail(qcItemId));
    }

    @PreAuthorize("@auth.hasPermission('quality:record:review')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody QcItemUpsertRequest request) {
        return AjaxResult.success(qcItemService.create(request));
    }

    @PreAuthorize("@auth.hasPermission('quality:record:review')")
    @PutMapping("/{qcItemId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "qcItemId must be greater than 0") Long qcItemId,
                             @Valid @RequestBody QcItemUpsertRequest request) {
        return AjaxResult.success(qcItemService.update(qcItemId, request));
    }

    @PreAuthorize("@auth.hasPermission('quality:record:review')")
    @PatchMapping("/{qcItemId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "qcItemId must be greater than 0") Long qcItemId,
                                  @Valid @RequestBody QcItemStatusPatchRequest request) {
        return AjaxResult.success(qcItemService.patchStatus(qcItemId, request));
    }
}
