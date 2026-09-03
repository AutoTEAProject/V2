package com.autotea.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final PythonEngineProperties pythonEngineProperties;

    /**
     * 계산 결과 xlsx가 base64로 실려오는 JSON 응답과, input 파일을 그대로 담는 multipart 요청 둘 다
     * 파일 하나 전체를 메모리에 올리므로 기본 256KB 버퍼 한도를 넉넉하게 늘려둔다.
     */
    @Bean
    public WebClient pythonEngineWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(pythonEngineProperties.timeoutSeconds()));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(pythonEngineProperties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
