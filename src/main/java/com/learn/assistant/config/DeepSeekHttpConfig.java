package com.learn.assistant.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

@Configuration
public class DeepSeekHttpConfig {

    @Bean
    public WebClientCustomizer systemDnsWebClientCustomizer() {
        HttpClient httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE);
        return builder -> builder.clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
