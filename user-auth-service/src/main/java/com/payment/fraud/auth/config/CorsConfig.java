package com.payment.fraud.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig {

    @Bean
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer corsConfigurer() {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override
            public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000") // React app
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}
