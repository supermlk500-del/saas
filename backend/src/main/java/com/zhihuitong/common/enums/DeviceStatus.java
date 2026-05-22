package com.zhihuitong.common.enums;

import com.zhihuitong.common.exception.BusinessException;

import java.util.Arrays;

public enum DeviceStatus {

    IDLE(1, "IDLE"),
    RUNNING(2, "RUNNING"),
    MAINTENANCE(3, "MAINTENANCE"),
    DISABLED(0, "DISABLED");

    private final int code;
    private final String apiValue;

    DeviceStatus(int code, String apiValue) {
        this.code = code;
        this.apiValue = apiValue;
    }

    public int getCode() {
        return code;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static int toCode(String apiValue) {
        return Arrays.stream(values())
                .filter(item -> item.apiValue.equalsIgnoreCase(apiValue))
                .findFirst()
                .orElseThrow(() -> new BusinessException(422, "Unsupported device status: " + apiValue))
                .code;
    }

    public static String fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .map(DeviceStatus::getApiValue)
                .orElse("UNKNOWN");
    }
}
