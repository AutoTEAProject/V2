package com.autotea.backend.web;

import com.autotea.backend.client.PythonEngineClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** utility 타입별 현재 단가(단위 포함). 케이스/run과 무관한 전역 설정값. */
@RestController
@RequiredArgsConstructor
public class UtilityPriceController {

    private final PythonEngineClient pythonEngineClient;

    @GetMapping("/api/utility-prices")
    public Map<String, PythonEngineClient.UtilityPrice> utilityPrices() {
        return pythonEngineClient.utilityPrices();
    }
}
