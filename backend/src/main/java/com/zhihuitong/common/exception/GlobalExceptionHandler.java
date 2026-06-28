package com.zhihuitong.common.exception;

import com.zhihuitong.common.domain.AjaxResult;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AjaxResult> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("参数校验失败");
        return buildResponse(400, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("参数绑定失败");
        return buildResponse(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AjaxResult> handleConstraintViolationException(ConstraintViolationException exception) {
        return buildResponse(400, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AjaxResult> handleHttpMessageNotReadableException() {
        return buildResponse(400, "请求体格式不正确");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AjaxResult> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return buildResponse(400, "参数不合法: " + exception.getName());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AjaxResult> handleAccessDeniedException() {
        return buildResponse(403, "无权执行该操作");
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AjaxResult> handleBusinessException(BusinessException exception) {
        return buildResponse(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException exception) {
        log.debug("Client disconnected before the response was written: {}", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleException(Exception exception) {
        log.error("Unhandled request exception", exception);
        return buildResponse(500, "服务器内部错误");
    }

    private ResponseEntity<AjaxResult> buildResponse(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(AjaxResult.error(code, message));
    }
}
