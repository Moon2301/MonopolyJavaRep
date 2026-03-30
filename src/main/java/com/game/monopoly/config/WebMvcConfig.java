package com.game.monopoly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ánh xạ tất cả các request có tiền tố /admin/** vào thư mục static/admin/
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/");
        
        // Mặc định ánh xạ các request root vào /static/
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
