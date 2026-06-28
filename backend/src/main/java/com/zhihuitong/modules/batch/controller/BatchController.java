package com.zhihuitong.modules.batch.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.batch.dto.BatchQuery;
import com.zhihuitong.modules.batch.dto.BatchUpsertRequest;
import com.zhihuitong.modules.batch.service.BatchResourcePoolService;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.batch.vo.BatchDetailVo;
import com.zhihuitong.modules.batch.vo.BatchImportResultVo;
import com.zhihuitong.modules.batch.vo.BatchListVo;
import com.zhihuitong.modules.batch.vo.BatchResourcePoolVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Validated
@RestController
@PreAuthorize("@auth.hasPermission('batch:resource:list')")
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;
    private final BatchResourcePoolService batchResourcePoolService;

    public BatchController(BatchService batchService, BatchResourcePoolService batchResourcePoolService) {
        this.batchService = batchService;
        this.batchResourcePoolService = batchResourcePoolService;
    }

    @GetMapping
    public TableDataInfo<BatchListVo> list(@Valid @ModelAttribute BatchQuery query) {
        return batchService.list(query);
    }

    @GetMapping("/resource-pool")
    public TableDataInfo<BatchResourcePoolVo> resourcePool(@Valid @ModelAttribute BatchQuery query) {
        return batchResourcePoolService.list(query);
    }

    @GetMapping("/{batchId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "batchId must be greater than 0") Long batchId) {
        BatchDetailVo detail = batchService.getDetail(batchId);
        return AjaxResult.success(detail);
    }

    @PreAuthorize("@auth.hasPermission('batch:resource:add')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody BatchUpsertRequest request) {
        return AjaxResult.success(batchService.create(request));
    }

    @PreAuthorize("@auth.hasPermission('batch:resource:edit')")
    @PutMapping("/{batchId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "batchId must be greater than 0") Long batchId,
                             @Valid @RequestBody BatchUpsertRequest request) {
        return AjaxResult.success(batchService.update(batchId, request));
    }

    @PreAuthorize("@auth.hasPermission('batch:resource:remove')")
    @DeleteMapping("/{batchId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "batchId must be greater than 0") Long batchId) {
        batchService.delete(batchId);
        return AjaxResult.success();
    }

    @PreAuthorize("@auth.hasPermission('batch:resource:add')")
    @PostMapping("/import")
    public AjaxResult importBatches(@RequestParam("file") MultipartFile file) throws IOException {
        BatchImportResultVo result = batchService.importBatches(file);
        return AjaxResult.success(result);
    }

    @GetMapping("/export")
    public void exportBatches(HttpServletResponse response) throws IOException {
        batchService.exportBatches(response);
    }
}
