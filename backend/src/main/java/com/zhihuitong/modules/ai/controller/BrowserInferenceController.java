package com.zhihuitong.modules.ai.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.ai.service.BrowserInferenceModelService;
import jakarta.validation.constraints.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai/browser-inference")
@PreAuthorize("@auth.hasPermission('quality:realtime:detect')")
public class BrowserInferenceController {

    private final BrowserInferenceModelService browserInferenceModelService;

    public BrowserInferenceController(BrowserInferenceModelService browserInferenceModelService) {
        this.browserInferenceModelService = browserInferenceModelService;
    }

    @GetMapping("/manifest")
    public AjaxResult manifest() {
        return AjaxResult.success(browserInferenceModelService.getManifest());
    }

    @GetMapping("/model")
    public ResponseEntity<Resource> model(
            @RequestParam
            @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "sha256 must be a 64 character hex string")
            String sha256
    ) {
        Resource resource = browserInferenceModelService.getModelResource(sha256);
        String activeSha256 = browserInferenceModelService.getActiveSha256();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(browserInferenceModelService.getModelFileSize())
                .eTag("\"" + activeSha256 + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=604800, immutable")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("best-" + activeSha256.substring(0, 12) + ".onnx").build().toString())
                .body(resource);
    }
}
