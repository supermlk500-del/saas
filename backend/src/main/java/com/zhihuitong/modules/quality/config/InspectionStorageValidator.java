package com.zhihuitong.modules.quality.config;

import com.zhihuitong.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class InspectionStorageValidator {

    private final InspectionStorageProperties properties;

    public InspectionStorageValidator(InspectionStorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validateDirectories() {
        ensureUsableDirectory(Path.of(properties.getUploadRoot()), "inspection.storage.upload-root");
        ensureUsableDirectory(Path.of(properties.getResultRoot()), "inspection.storage.result-root");
    }

    private void ensureUsableDirectory(Path path, String propertyName) {
        try {
            Files.createDirectories(path);
            if (!Files.isDirectory(path) || !Files.isWritable(path)) {
                throw new BusinessException(500, propertyName + " is not a writable directory: " + path);
            }
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to initialize storage directory for " + propertyName + ": " + exception.getMessage());
        }
    }
}
