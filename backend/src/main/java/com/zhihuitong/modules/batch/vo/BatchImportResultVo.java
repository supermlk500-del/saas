package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchImportResultVo {

    private int totalCount;

    private int successCount;

    private int failCount;

    private List<String> errors = new ArrayList<>();
}
