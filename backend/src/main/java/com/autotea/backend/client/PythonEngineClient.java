package com.autotea.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PythonEngineClient {

    private final WebClient pythonEngineWebClient;

    public CalculateResponse calculate(String runId) {
        CalculateRequest request = new CalculateRequest(runId);
        return pythonEngineWebClient.post()
                .uri("/calculate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CalculateResponse.class)
                .block();
    }

    public ParseResponse parse(String runId) {
        CalculateRequest request = new CalculateRequest(runId);
        return pythonEngineWebClient.post()
                .uri("/parse")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ParseResponse.class)
                .block();
    }

    public record CalculateRequest(@JsonProperty("runId") String runId) {
    }

    public record CalculateResponse(
            String status,
            String resultPath,
            String logs,
            String errorMessage
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    public record ParseResponse(
            String status,
            List<EquipmentInfo> equipment,
            String logs,
            String errorMessage
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    public record EquipmentInfo(String name, String type) {
    }
}
