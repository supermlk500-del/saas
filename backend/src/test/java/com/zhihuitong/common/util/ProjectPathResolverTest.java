package com.zhihuitong.common.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectPathResolverTest {

    @Test
    void resolvesRepositoryRelativeModelPathFromTheCurrentCheckout() {
        Path resolved = ProjectPathResolver.resolve("docs/best.onnx");

        assertEquals("best.onnx", resolved.getFileName().toString());
        assertTrue(Files.isRegularFile(resolved), "The repository model must be found without an absolute local path");
        assertTrue(Files.isDirectory(resolved.getParent().getParent().resolve("backend")));
    }

    @Test
    void preservesAbsoluteDeploymentPaths() {
        Path configured = Path.of(System.getProperty("java.io.tmpdir"), "zhihuitong-model.onnx");

        assertEquals(configured.normalize(), ProjectPathResolver.resolve(configured.toString()));
    }
}
