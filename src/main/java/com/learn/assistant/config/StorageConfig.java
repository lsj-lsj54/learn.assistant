package com.learn.assistant.config;

import com.learn.assistant.properties.PathProperties;
import com.learn.assistant.properties.ToolProperties;
import com.learn.assistant.tool.path.ProjectPaths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PathProperties.class, ToolProperties.class})
public class StorageConfig {

    @Bean
    public ProjectPaths projectPaths(PathProperties pathProperties) {
        return new ProjectPaths(pathProperties.getPdfDir(), pathProperties.getDownloadDir());
    }
}
