package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.ai.dto.BrowserInferenceClassItem;
import com.zhihuitong.modules.ai.dto.BrowserInferenceManifestResponse;
import com.zhihuitong.modules.ai.service.BrowserInferenceModelService;
import com.zhihuitong.modules.quality.dto.ClientDetectionBox;
import com.zhihuitong.modules.quality.dto.ClientImageDetectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientInferenceResultValidatorTest {

    private static final String ACTIVE_SHA = "a".repeat(64);

    private BrowserInferenceModelService modelService;
    private ClientInferenceResultValidator validator;

    @BeforeEach
    void setUp() {
        modelService = mock(BrowserInferenceModelService.class);
        BrowserInferenceManifestResponse manifest = new BrowserInferenceManifestResponse();
        manifest.setClasses(List.of(
                new BrowserInferenceClassItem(0, "hole", "破洞", "#ef4444"),
                new BrowserInferenceClassItem(1, "stain", "污渍", "#f97316")
        ));
        when(modelService.getActiveSha256()).thenReturn(ACTIVE_SHA);
        when(modelService.getManifest()).thenReturn(manifest);
        validator = new ClientInferenceResultValidator(modelService);
    }

    @Test
    void validateImageResult_acceptsMatchingImagesAndBoxes() throws IOException {
        ClientImageDetectionRequest request = validRequest();

        var classes = validator.validateImageResult(
                request,
                image("source.png", 10, 8),
                image("result.png", 10, 8)
        );

        assertEquals("hole", classes.get(0).getCode());
    }

    @Test
    void validateImageResult_rejectsInactiveModelSha() throws IOException {
        ClientImageDetectionRequest request = validRequest();
        request.setModelSha256("b".repeat(64));

        assertThrows(BusinessException.class, () -> validator.validateImageResult(
                request,
                image("source.png", 10, 8),
                image("result.png", 10, 8)
        ));
    }

    @Test
    void validateImageResult_rejectsUnknownClassMapping() throws IOException {
        ClientImageDetectionRequest request = validRequest();
        request.getDetections().get(0).setCode("unknown");

        assertThrows(BusinessException.class, () -> validator.validateImageResult(
                request,
                image("source.png", 10, 8),
                image("result.png", 10, 8)
        ));
    }

    @Test
    void validateImageResult_rejectsOutOfBoundsBox() throws IOException {
        ClientImageDetectionRequest request = validRequest();
        request.getDetections().get(0).setX2(11.0);

        assertThrows(BusinessException.class, () -> validator.validateImageResult(
                request,
                image("source.png", 10, 8),
                image("result.png", 10, 8)
        ));
    }

    @Test
    void validateImageResult_rejectsScoreOutsideUnitRange() throws IOException {
        ClientImageDetectionRequest request = validRequest();
        request.getDetections().get(0).setScore(1.01);

        assertThrows(BusinessException.class, () -> validator.validateImageResult(
                request,
                image("source.png", 10, 8),
                image("result.png", 10, 8)
        ));
    }

    @Test
    void validateImageResult_rejectsPayloadAndFileDimensionMismatch() throws IOException {
        ClientImageDetectionRequest request = validRequest();

        assertThrows(BusinessException.class, () -> validator.validateImageResult(
                request,
                image("source.png", 12, 8),
                image("result.png", 12, 8)
        ));
    }

    @Test
    void validateImageResult_rejectsSourceAndResultDimensionMismatch() throws IOException {
        ClientImageDetectionRequest request = validRequest();

        assertThrows(BusinessException.class, () -> validator.validateImageResult(
                request,
                image("source.png", 10, 8),
                image("result.png", 10, 7)
        ));
    }

    private ClientImageDetectionRequest validRequest() {
        ClientDetectionBox box = new ClientDetectionBox();
        box.setClassIndex(0);
        box.setCode("hole");
        box.setLabel("破洞");
        box.setScore(0.92);
        box.setX1(1.0);
        box.setY1(1.0);
        box.setX2(9.0);
        box.setY2(7.0);

        ClientImageDetectionRequest request = new ClientImageDetectionRequest();
        request.setPlanStepId(1L);
        request.setQcItemId(1L);
        request.setModelSha256(ACTIVE_SHA);
        request.setImageWidth(10);
        request.setImageHeight(8);
        request.setPreprocessTimeMs(1);
        request.setInferenceTimeMs(2);
        request.setPostprocessTimeMs(1);
        request.setProviderStrategy("WASM");
        request.setDetections(List.of(box));
        return request;
    }

    private MockMultipartFile image(String name, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile(name, name, "image/png", output.toByteArray());
    }
}
