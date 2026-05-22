package com.zhihuitong.modules.exception.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.exception.dto.ExceptionCloseRequest;
import com.zhihuitong.modules.exception.dto.ExceptionRecordQuery;
import com.zhihuitong.modules.exception.dto.ExceptionRecordUpsertRequest;
import com.zhihuitong.modules.exception.dto.ExceptionReworkRequest;
import com.zhihuitong.modules.exception.dto.ExceptionStatusPatchRequest;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.service.ExceptionRecordService;
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
@RequestMapping("/api/exception-records")
public class ExceptionRecordController {

    private final ExceptionRecordService exceptionRecordService;

    public ExceptionRecordController(ExceptionRecordService exceptionRecordService) {
        this.exceptionRecordService = exceptionRecordService;
    }

    @GetMapping
    public TableDataInfo<ExceptionRecord> list(@Valid @ModelAttribute ExceptionRecordQuery query) {
        return exceptionRecordService.list(query);
    }

    @GetMapping("/{exceptionId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "exceptionId must be greater than 0") Long exceptionId) {
        return AjaxResult.success(exceptionRecordService.getDetail(exceptionId));
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody ExceptionRecordUpsertRequest request) {
        return AjaxResult.success(exceptionRecordService.create(request));
    }

    @PutMapping("/{exceptionId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "exceptionId must be greater than 0") Long exceptionId,
                             @Valid @RequestBody ExceptionRecordUpsertRequest request) {
        return AjaxResult.success(exceptionRecordService.update(exceptionId, request));
    }

    @PatchMapping("/{exceptionId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "exceptionId must be greater than 0") Long exceptionId,
                                  @Valid @RequestBody ExceptionStatusPatchRequest request) {
        return AjaxResult.success(exceptionRecordService.patchStatus(exceptionId, request));
    }

    @PatchMapping("/{exceptionId}/close")
    public AjaxResult close(@PathVariable @Min(value = 1, message = "exceptionId must be greater than 0") Long exceptionId,
                            @Valid @RequestBody ExceptionCloseRequest request) {
        return AjaxResult.success(exceptionRecordService.close(exceptionId, request));
    }

    @PostMapping("/{exceptionId}/rework")
    public AjaxResult rework(@PathVariable @Min(value = 1, message = "exceptionId must be greater than 0") Long exceptionId,
                             @Valid @RequestBody ExceptionReworkRequest request) {
        return AjaxResult.success(exceptionRecordService.rework(exceptionId, request));
    }
}
