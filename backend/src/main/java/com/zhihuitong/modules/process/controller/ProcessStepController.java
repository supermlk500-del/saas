package com.zhihuitong.modules.process.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.process.dto.IsActivePatchRequest;
import com.zhihuitong.modules.process.dto.ProcessStepQuery;
import com.zhihuitong.modules.process.dto.ProcessStepUpsertRequest;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.service.ProcessStepService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/process-steps")
public class ProcessStepController {

    private final ProcessStepService processStepService;

    public ProcessStepController(ProcessStepService processStepService) {
        this.processStepService = processStepService;
    }

    @GetMapping
    public TableDataInfo<ProcessStep> list(@Valid @ModelAttribute ProcessStepQuery query) {
        return processStepService.list(query);
    }

    @GetMapping("/{stepId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "stepId must be greater than 0") Long stepId) {
        return AjaxResult.success(processStepService.getDetail(stepId));
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody ProcessStepUpsertRequest request) {
        return AjaxResult.success(processStepService.create(request));
    }

    @PutMapping("/{stepId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "stepId must be greater than 0") Long stepId,
                             @Valid @RequestBody ProcessStepUpsertRequest request) {
        return AjaxResult.success(processStepService.update(stepId, request));
    }

    @PatchMapping("/{stepId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "stepId must be greater than 0") Long stepId,
                                  @Valid @RequestBody IsActivePatchRequest request) {
        return AjaxResult.success(processStepService.patchStatus(stepId, request));
    }

    @DeleteMapping("/{stepId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "stepId must be greater than 0") Long stepId) {
        processStepService.delete(stepId);
        return AjaxResult.success();
    }
}
