package com.zhihuitong.controller;

import com.zhihuitong.common.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public AjaxResult health() {
        return AjaxResult.success(Map.of(
                "status", "UP",
                "service", "zhihuitong-backend",
                "time", LocalDateTime.now().toString()
        ));
    }
}

