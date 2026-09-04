package com.autotea.backend.web;

import com.autotea.backend.domain.UtilityType;
import com.autotea.backend.dto.UtilityPriceResponse;
import com.autotea.backend.dto.UtilityPriceUpdateRequest;
import com.autotea.backend.service.UtilityPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** utility 타입별 현재 단가(단위 포함). 케이스/run과 무관한 전역 설정값이라 수식 라이브러리처럼 자유롭게 수정 가능. */
@RestController
@RequestMapping("/api/utility-prices")
@RequiredArgsConstructor
public class UtilityPriceController {

    private final UtilityPriceService utilityPriceService;

    @GetMapping
    public List<UtilityPriceResponse> findAll() {
        return utilityPriceService.findAll().stream().map(UtilityPriceResponse::from).toList();
    }

    @PutMapping("/{utilityType}")
    public UtilityPriceResponse update(@PathVariable UtilityType utilityType, @Valid @RequestBody UtilityPriceUpdateRequest request) {
        return UtilityPriceResponse.from(utilityPriceService.update(utilityType, request.value()));
    }
}
