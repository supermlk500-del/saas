package com.zhihuitong.modules.process.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.process.dto.MachineQuery;
import com.zhihuitong.modules.process.dto.MachineStatusPatchRequest;
import com.zhihuitong.modules.process.dto.MachineUpsertRequest;
import com.zhihuitong.modules.process.service.MachineService;
import com.zhihuitong.modules.process.vo.MachineVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("@auth.hasPermission('process:machine:list')")
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    public TableDataInfo<MachineVo> list(@Valid @ModelAttribute MachineQuery query) {
        return machineService.list(query);
    }

    @GetMapping("/{machineId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "machineId must be greater than 0") Long machineId) {
        return AjaxResult.success(machineService.getDetail(machineId));
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody MachineUpsertRequest request) {
        return AjaxResult.success(machineService.create(request));
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @PutMapping("/{machineId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "machineId must be greater than 0") Long machineId,
                             @Valid @RequestBody MachineUpsertRequest request) {
        return AjaxResult.success(machineService.update(machineId, request));
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @PatchMapping("/{machineId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "machineId must be greater than 0") Long machineId,
                                  @Valid @RequestBody MachineStatusPatchRequest request) {
        return AjaxResult.success(machineService.patchStatus(machineId, request));
    }

    @PreAuthorize("@auth.hasPermission('process:machine:edit')")
    @DeleteMapping("/{machineId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "machineId must be greater than 0") Long machineId) {
        machineService.delete(machineId);
        return AjaxResult.success();
    }
}
