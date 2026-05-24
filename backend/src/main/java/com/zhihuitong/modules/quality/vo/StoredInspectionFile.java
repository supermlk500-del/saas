package com.zhihuitong.modules.quality.vo;

import lombok.Data;

import java.nio.file.Path;

@Data
public class StoredInspectionFile {

    private String fileName;

    private String relativePath;

    private Path absolutePath;
}
