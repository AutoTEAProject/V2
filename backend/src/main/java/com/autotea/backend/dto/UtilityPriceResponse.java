package com.autotea.backend.dto;

import com.autotea.backend.domain.UtilityPriceSetting;
import com.autotea.backend.domain.UtilityType;

public record UtilityPriceResponse(
        UtilityType utilityType,
        Double value,
        String unit
) {
    public static UtilityPriceResponse from(UtilityPriceSetting setting) {
        return new UtilityPriceResponse(setting.getUtilityType(), setting.getValue(), setting.getUnit());
    }
}
