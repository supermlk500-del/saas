package com.zhihuitong.modules.quality.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.quality.dto.InspectionDataQuery;
import com.zhihuitong.modules.quality.dto.InspectionDataUpsertRequest;
import com.zhihuitong.modules.quality.entity.InspectionData;
import com.zhihuitong.modules.quality.service.InspectionDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("@auth.hasPermission('quality:realtime:view')")
@RequestMapping("/api/inspection-data")
public class InspectionDataController {

    private final InspectionDataService inspectionDataService;

    public InspectionDataController(InspectionDataService inspectionDataService) {
        this.inspectionDataService = inspectionDataService;
    }

    @GetMapping
    public TableDataInfo<InspectionData> list(@Valid @ModelAttribute InspectionDataQuery query) {
        return inspectionDataService.list(query);
    }

    @PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody InspectionDataUpsertRequest request) {
        return AjaxResult.success(inspectionDataService.create(request));
    }

    @PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
    @DeleteMapping("/{dataId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "dataId must be greater than 0") Long dataId) {
        inspectionDataService.delete(dataId);
        return AjaxResult.success();
    }
}
