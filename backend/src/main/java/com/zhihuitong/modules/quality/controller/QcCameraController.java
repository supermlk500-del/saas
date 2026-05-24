package com.zhihuitong.modules.quality.controller;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.quality.dto.QcCameraQuery;
import com.zhihuitong.modules.quality.entity.QcCamera;
import com.zhihuitong.modules.quality.service.QcCameraService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/qc-cameras")
public class QcCameraController {

    private final QcCameraService qcCameraService;

    public QcCameraController(QcCameraService qcCameraService) {
        this.qcCameraService = qcCameraService;
    }

    @GetMapping
    public TableDataInfo<QcCamera> list(@Valid @ModelAttribute QcCameraQuery query) {
        return qcCameraService.list(query);
    }
}
