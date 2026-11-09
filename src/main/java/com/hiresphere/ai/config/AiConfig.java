package com.hiresphere.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({AiProperties.class, ResilienceProperties.class, AiLimitsProperties.class})
public class AiConfig {

    @Bean("geminiRestClient")
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    RestClient geminiRestClient(AiProperties props) {
        AiProperties.Gemini gemini = props.gemini();
        HttpClient http = HttpClient.newBuilder().connectTimeout(gemini.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(gemini.readTimeout());
        return RestClient.builder().baseUrl(gemini.baseUrl()).requestFactory(factory).build();
    }
}