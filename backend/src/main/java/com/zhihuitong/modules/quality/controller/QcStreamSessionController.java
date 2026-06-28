package com.zhihuitong.modules.quality.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.quality.dto.QcStreamSessionCreateRequest;
import com.zhihuitong.modules.quality.dto.QcStreamSnapshotRequest;
import com.zhihuitong.modules.quality.service.QcStreamSessionService;
import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import com.zhihuitong.modules.quality.vo.QcStreamSessionVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
@RequestMapping("/api/qc-stream-sessions")
public class QcStreamSessionController {

    private final QcStreamSessionService qcStreamSessionService;

    public QcStreamSessionController(QcStreamSessionService qcStreamSessionService) {
        this.qcStreamSessionService = qcStreamSessionService;
    }

    @PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody QcStreamSessionCreateRequest request) {
        QcStreamSessionVo session = qcStreamSessionService.createSession(request);
        return AjaxResult.success(session);
    }

    @PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
    @PostMapping(value = "/{sessionId}/snapshot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult snapshot(@PathVariable @NotBlank(message = "sessionId must not be blank") String sessionId,
                               @Valid @ModelAttribute QcStreamSnapshotRequest request) {
        InspectionIntegrationResultVo result = qcStreamSessionService.snapshot(sessionId, request);
        return AjaxResult.success(result);
    }

    @PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
    @PostMapping("/{sessionId}/close")
    public AjaxResult close(@PathVariable @NotBlank(message = "sessionId must not be blank") String sessionId) {
        qcStreamSessionService.closeSession(sessionId);
        return AjaxResult.success();
    }
}
