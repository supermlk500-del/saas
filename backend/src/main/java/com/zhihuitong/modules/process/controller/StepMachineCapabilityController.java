package com.zhihuitong.modules.process.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.process.dto.CapabilityQuery;
import com.zhihuitong.modules.process.dto.CapabilityUpsertRequest;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.service.StepMachineCapabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("@auth.hasPermission('process:machine:list')")
@RequestMapping("/api/step-machine-capabilities")
public class StepMachineCapabilityController {

    private final StepMachineCapabilityService capabilityService;

    public StepMachineCapabilityController(StepMachineCapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    @GetMapping
    public TableDataInfo<StepMachineCapability> list(@Valid @ModelAttribute CapabilityQuery query) {
        return capabilityService.list(query);
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody CapabilityUpsertRequest request) {
        return AjaxResult.success(capabilityService.create(request));
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @PutMapping("/{capId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "capId must be greater than 0") Long capId,
                             @Valid @RequestBody CapabilityUpsertRequest request) {
        return AjaxResult.success(capabilityService.update(capId, request));
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @DeleteMapping("/{capId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "capId must be greater than 0") Long capId) {
        capabilityService.delete(capId);
        return AjaxResult.success();
    }
}
