package com.zhihuitong.modules.quality.controller;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.quality.dto.QcTaskQuery;
import com.zhihuitong.modules.quality.service.QcTaskService;
import com.zhihuitong.modules.quality.vo.QcTaskVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("@auth.hasPermission('quality:realtime:view')")
@RequestMapping("/api/qc-tasks")
public class QcTaskController {

    private final QcTaskService qcTaskService;

    public QcTaskController(QcTaskService qcTaskService) {
        this.qcTaskService = qcTaskService;
    }

    @GetMapping
    public TableDataInfo<QcTaskVo> list(@Valid @ModelAttribute QcTaskQuery query) {
        return qcTaskService.list(query);
    }
}
