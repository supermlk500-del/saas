package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.quality.config.InspectionStorageProperties;
import com.zhihuitong.modules.quality.vo.StoredInspectionFile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
public class InspectionFileStorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final InspectionStorageProperties storageProperties;

    public InspectionFileStorageService(InspectionStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public StoredInspectionFile storeSourceImage(MultipartFile file) {
        return store(file, Path.of(storageProperties.getUploadRoot()), "photo/upload", "source");
    }

    public StoredInspectionFile storeResultImage(MultipartFile file) {
        return store(file, Path.of(storageProperties.getResultRoot()), "photo/results", "result");
    }

    public StoredInspectionFile storeSourceImage(byte[] imageBytes, String extension) {
        return store(imageBytes, Path.of(storageProperties.getUploadRoot()), "photo/upload", "source", extension);
    }

    public StoredInspectionFile prepareResultImageTarget(String extension) {
        return prepareTarget(Path.of(storageProperties.getResultRoot()), "photo/results", "result", extension);
    }

    public void writeRenderedImage(BufferedImage image, StoredInspectionFile targetFile) {
        if (image == null) {
            throw new BusinessException(500, "Rendered inspection image must not be null");
        }
        if (targetFile == null || targetFile.getAbsolutePath() == null) {
            throw new BusinessException(500, "Rendered inspection target file must not be null");
        }
        try {
            Files.createDirectories(targetFile.getAbsolutePath().getParent());
            String formatName = resolveFormatName(targetFile.getFileName());
            ImageIO.write(image, formatName, targetFile.getAbsolutePath().toFile());
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to write rendered inspection image: " + exception.getMessage());
        }
    }

    public String normalizeResultRelativePath(String path) {
        return normalizeRelativePath(path, Path.of(storageProperties.getResultRoot()), "photo/results");
    }

    public String normalizeUploadRelativePath(String path) {
        return normalizeRelativePath(path, Path.of(storageProperties.getUploadRoot()), "photo/upload");
    }

    private StoredInspectionFile store(MultipartFile file, Path rootPath, String relativePrefix, String logicalPrefix) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "file must not be empty");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        StoredInspectionFile targetFile = prepareTarget(rootPath, relativePrefix, logicalPrefix, extension);
        try {
            Files.createDirectories(targetFile.getAbsolutePath().getParent());
            Files.copy(file.getInputStream(), targetFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to store inspection file: " + exception.getMessage());
        }
        return targetFile;
    }

    private StoredInspectionFile store(byte[] imageBytes,
                                       Path rootPath,
                                       String relativePrefix,
                                       String logicalPrefix,
                                       String extension) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(400, "image bytes must not be empty");
        }
        StoredInspectionFile targetFile = prepareTarget(rootPath, relativePrefix, logicalPrefix, extension);
        try {
            Files.createDirectories(targetFile.getAbsolutePath().getParent());
            Files.write(targetFile.getAbsolutePath(), imageBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to store inspection file: " + exception.getMessage());
        }
        return targetFile;
    }

    private StoredInspectionFile prepareTarget(Path rootPath, String relativePrefix, String logicalPrefix, String extension) {
        String dateFolder = DATE_FORMATTER.format(LocalDate.now());
        String normalizedExtension = normalizeExtension(extension);
        String fileName = logicalPrefix + "_" + TIMESTAMP_FORMATTER.format(LocalDateTime.now()) + "_" + shortUuid() + normalizedExtension;
        Path dayDirectory = rootPath.resolve(dateFolder);
        Path absolutePath = dayDirectory.resolve(fileName);
        StoredInspectionFile storedFile = new StoredInspectionFile();
        storedFile.setFileName(fileName);
        storedFile.setAbsolutePath(absolutePath);
        storedFile.setRelativePath(relativePrefix + "/" + dateFolder + "/" + fileName);
        return storedFile;
    }

    private String normalizeRelativePath(String rawPath, Path rootPath, String relativePrefix) {
        if (!StringUtils.hasText(rawPath)) {
            return rawPath;
        }
        String normalized = rawPath.replace("\\", "/").trim();
        if (normalized.startsWith(relativePrefix + "/") || normalized.equals(relativePrefix)) {
            return normalized;
        }
        String normalizedRoot = rootPath.toString().replace("\\", "/");
        if (normalized.startsWith(normalizedRoot + "/")) {
            return relativePrefix + normalized.substring(normalizedRoot.length());
        }
        if (normalized.startsWith("/" + relativePrefix + "/")) {
            return normalized.substring(1);
        }
        if (normalized.startsWith("results/")) {
            return "photo/" + normalized;
        }
        if (normalized.startsWith("upload/")) {
            return "photo/" + normalized;
        }
        if (normalized.startsWith("photo/")) {
            return normalized;
        }
        return relativePrefix + "/" + extractFileName(normalized);
    }

    private String resolveExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return ".jpg";
        }
        int index = originalFileName.lastIndexOf('.');
        if (index < 0 || index == originalFileName.length() - 1) {
            return ".jpg";
        }
        return originalFileName.substring(index);
    }

    private String normalizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return ".jpg";
        }
        return extension.startsWith(".") ? extension : "." + extension;
    }

    private String resolveFormatName(String fileName) {
        String extension = resolveExtension(fileName);
        return extension.substring(1).toLowerCase();
    }

    private String extractFileName(String path) {
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
