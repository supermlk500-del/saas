package com.zhihuitong.modules.batch.enums;

public enum BatchResourceStatusEnum {

    UNALLOCATED("UNALLOCATED", "待分配"),
    PARTIALLY_ALLOCATED("PARTIALLY_ALLOCATED", "部分分配"),
    ALLOCATED("ALLOCATED", "已分配"),
    IN_EXECUTION("IN_EXECUTION", "执行中"),
    CONSUMED("CONSUMED", "已消耗"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    BatchResourceStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
