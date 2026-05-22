package com.zhihuitong.common.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(422, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
