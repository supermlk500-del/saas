package com.zhihuitong.config;

import com.zhihuitong.modules.quality.config.InspectionStorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final InspectionStorageProperties inspectionStorageProperties;

    public WebMvcConfig(InspectionStorageProperties inspectionStorageProperties) {
        this.inspectionStorageProperties = inspectionStorageProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/photo/upload/**")
                .addResourceLocations(toFileResourceLocation(inspectionStorageProperties.getUploadRoot()));
        registry.addResourceHandler("/photo/results/**")
                .addResourceLocations(toFileResourceLocation(inspectionStorageProperties.getResultRoot()));
    }

    private String toFileResourceLocation(String path) {
        return Path.of(path).toUri().toString();
    }
}
