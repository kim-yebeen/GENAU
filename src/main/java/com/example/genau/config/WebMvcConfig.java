// WebMvcConfig.java

package com.example.genau.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 유저 프로필 이미지
        registry.addResourceHandler("/uploads/profiles/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/profiles/");

        // 팀 프로필 이미지 (동적 경로)
        registry.addResourceHandler("/uploads/teams/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/teams/");
    }
}




