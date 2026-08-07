package com.zhihuitong.modules.ai.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.ProjectPathResolver;
import com.zhihuitong.modules.ai.config.AiYoloProperties;
import com.zhihuitong.modules.ai.dto.BrowserInferenceManifestResponse;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserInferenceModelServiceTest {

    @Test
    void getManifest_inspectsRealOnnxModel() {
        Path modelPath = ProjectPathResolver.resolve("docs/best.onnx");
        assertTrue(Files.isRegularFile(modelPath), "docs/best.onnx must exist for browser inference");

        AiYoloProperties properties = new AiYoloProperties();
        properties.setModelPath(modelPath.toString());
        BrowserInferenceModelService service = new BrowserInferenceModelService(properties);

        BrowserInferenceManifestResponse manifest = service.getManifest();

        assertEquals(64, manifest.getSha256().length());
        assertEquals(Files.exists(modelPath) ? modelPath.getFileName().toString() : "best.onnx", manifest.getModelName());
        assertTrue(manifest.getFileSize() > 0);
        assertEquals(640, manifest.getInputWidth());
        assertEquals(640, manifest.getInputHeight());
        assertEquals("images", manifest.getInputName());
        assertEquals("output0", manifest.getOutputName());
        assertNotNull(manifest.getDecoder());
        assertEquals("yolo-raw", manifest.getDecoder().getType());
        assertEquals("BCN", manifest.getDecoder().getOutputLayout());
        assertEquals("cxcywh", manifest.getDecoder().getBoxFormat());
        assertFalse(manifest.getDecoder().isHasObjectness());
        assertEquals(20, manifest.getClasses().size());
        assertEquals("hole", manifest.getClasses().get(0).getCode());
        assertEquals("other_defect", manifest.getClasses().get(19).getCode());
        assertTrue(manifest.getModelUrl().contains(manifest.getSha256()));
    }

    @Test
    void getModelResource_rejectsInactiveSha() {
        Path modelPath = ProjectPathResolver.resolve("docs/best.onnx");
        AiYoloProperties properties = new AiYoloProperties();
        properties.setModelPath(modelPath.toString());
        BrowserInferenceModelService service = new BrowserInferenceModelService(properties);

        assertThrows(BusinessException.class, () -> service.getModelResource("0".repeat(64)));
    }
}
