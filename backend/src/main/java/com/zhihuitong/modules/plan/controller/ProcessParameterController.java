package com.zhihuitong.modules.plan.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.ProcessParameterQuery;
import com.zhihuitong.modules.plan.dto.ProcessParameterUpsertRequest;
import com.zhihuitong.modules.plan.entity.ProcessParameter;
import com.zhihuitong.modules.plan.service.ProcessParameterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/process-parameters")
public class ProcessParameterController {

    private final ProcessParameterService processParameterService;

    public ProcessParameterController(ProcessParameterService processParameterService) {
        this.processParameterService = processParameterService;
    }

    @GetMapping
    public TableDataInfo<ProcessParameter> list(@Valid @ModelAttribute ProcessParameterQuery query) {
        return processParameterService.list(query);
    }

    @GetMapping("/{paramId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "paramId must be greater than 0") Long paramId) {
        return AjaxResult.success(processParameterService.getDetail(paramId));
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody ProcessParameterUpsertRequest request) {
        return AjaxResult.success(processParameterService.create(request));
    }

    @PutMapping("/{paramId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "paramId must be greater than 0") Long paramId,
                             @Valid @RequestBody ProcessParameterUpsertRequest request) {
        return AjaxResult.success(processParameterService.update(paramId, request));
    }

    @DeleteMapping("/{paramId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "paramId must be greater than 0") Long paramId) {
        processParameterService.delete(paramId);
        return AjaxResult.success();
    }
}
