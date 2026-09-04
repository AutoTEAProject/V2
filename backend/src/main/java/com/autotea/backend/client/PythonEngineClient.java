package com.autotea.backend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * python-engine과는 디스크 공유 없이 요청/응답 바디로만 파일을 주고받는다
 * (배포 환경에서 두 서비스가 볼륨을 공유할 수 없다는 전제).
 */
@Component
@RequiredArgsConstructor
public class PythonEngineClient {

    private final WebClient pythonEngineWebClient;

    public ParseResponse parse(byte[] xlsxBytes, byte[] repBytes) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("xlsxFile", new ByteArrayResource(xlsxBytes)).filename("input.xlsx");
        builder.part("repFile", new ByteArrayResource(repBytes)).filename("input.rep");

        return pythonEngineWebClient.post()
                .uri("/parse")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(ParseResponse.class)
                .block();
    }

    public CalculateResponse calculate(byte[] xlsxBytes, byte[] repBytes, String equipmentConfigJson) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("xlsxFile", new ByteArrayResource(xlsxBytes)).filename("input.xlsx");
        builder.part("repFile", new ByteArrayResource(repBytes)).filename("input.rep");
        builder.part("equipmentConfig", equipmentConfigJson);

        return pythonEngineWebClient.post()
                .uri("/calculate")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CalculateResponse.class)
                .block();
    }

    public Map<String, UtilityPrice> utilityPrices() {
        return pythonEngineWebClient.get()
                .uri("/config/utility-prices")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, UtilityPrice>>() {
                })
                .block();
    }

    public record ParseResponse(
            String status,
            List<EquipmentInfo> equipment,
            List<String> streams,
            String logs,
            String errorMessage
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    public record CalculateResponse(
            String status,
            /** output.xlsx 바이트를 base64로 인코딩한 것(공유 디스크 없이 응답 바디로 돌려받기 위함) */
            String outputXlsxBase64,
            /** 장치 이름 -> (수식 이름 -> {K1,K2,K3,"EQUIPMENT COST",...}) */
            Map<String, Map<String, Map<String, Object>>> costResult,
            String logs,
            String errorMessage
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    public record EquipmentInfo(String name, String type) {
    }

    public record UtilityPrice(double value, String unit) {
    }
}
