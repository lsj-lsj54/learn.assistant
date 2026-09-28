package com.learn.assistant.config;

import com.learn.assistant.web.HideSwaggerUiFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<HideSwaggerUiFilter> hideSwaggerUiFilter() {
        FilterRegistrationBean<HideSwaggerUiFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HideSwaggerUiFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*", "null")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
