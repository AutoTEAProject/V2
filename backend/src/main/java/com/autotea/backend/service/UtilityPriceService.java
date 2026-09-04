package com.autotea.backend.service;

import com.autotea.backend.domain.UtilityPriceSetting;
import com.autotea.backend.domain.UtilityType;
import com.autotea.backend.exception.ResourceNotFoundException;
import com.autotea.backend.repository.UtilityPriceSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UtilityPriceService {

    private final UtilityPriceSettingRepository utilityPriceSettingRepository;

    public List<UtilityPriceSetting> findAll() {
        return utilityPriceSettingRepository.findAll();
    }

    public UtilityPriceSetting update(UtilityType utilityType, Double value) {
        UtilityPriceSetting setting = utilityPriceSettingRepository.findById(utilityType)
                .orElseThrow(() -> new ResourceNotFoundException("Utility price not found: " + utilityType));
        setting.setValue(value);
        return utilityPriceSettingRepository.save(setting);
    }

    /** run 실행 시 python-engine에 넘길 equipment_config.json의 "utilityPrices" 값. */
    public Map<String, Object> buildConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        for (UtilityPriceSetting setting : utilityPriceSettingRepository.findAll()) {
            config.put(setting.getUtilityType().name(), Map.of("value", setting.getValue(), "unit", setting.getUnit()));
        }
        return config;
    }
}
