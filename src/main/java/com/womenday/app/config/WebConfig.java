package com.womenday.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve ảnh upload từ thư mục uploads/images/ ra URL /images/**
        String uploadPath = Paths.get("uploads/images/").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath);
    }
}