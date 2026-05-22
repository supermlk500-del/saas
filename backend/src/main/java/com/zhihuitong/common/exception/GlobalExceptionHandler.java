package com.zhihuitong.common.exception;

import com.zhihuitong.common.domain.AjaxResult;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return AjaxResult.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Binding failed");
        return AjaxResult.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult handleConstraintViolationException(ConstraintViolationException exception) {
        return AjaxResult.error(400, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AjaxResult handleHttpMessageNotReadableException() {
        return AjaxResult.error(400, "Invalid request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return AjaxResult.error(400, "Invalid parameter: " + exception.getName());
    }

    @ExceptionHandler(BusinessException.class)
    public AjaxResult handleBusinessException(BusinessException exception) {
        return AjaxResult.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception exception) {
        return AjaxResult.error(exception.getMessage() == null ? "Internal server error" : exception.getMessage());
    }
}
