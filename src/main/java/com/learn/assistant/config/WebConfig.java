package com.learn.assistant.config;

import com.learn.assistant.web.HideSwaggerUiFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
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

    @Bean
    public AsyncTaskExecutor chatStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("chat-stream-");
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(chatStreamExecutor());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*", "null")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
