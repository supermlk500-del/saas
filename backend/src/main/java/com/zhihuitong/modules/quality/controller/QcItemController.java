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

    @PostMapping
    public AjaxResult create(@Valid @RequestBody QcItemUpsertRequest request) {
        return AjaxResult.success(qcItemService.create(request));
    }

    @PutMapping("/{qcItemId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "qcItemId must be greater than 0") Long qcItemId,
                             @Valid @RequestBody QcItemUpsertRequest request) {
        return AjaxResult.success(qcItemService.update(qcItemId, request));
    }

    @PatchMapping("/{qcItemId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "qcItemId must be greater than 0") Long qcItemId,
                                  @Valid @RequestBody QcItemStatusPatchRequest request) {
        return AjaxResult.success(qcItemService.patchStatus(qcItemId, request));
    }
}
