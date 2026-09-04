package com.autotea.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UtilityPriceUpdateRequest(
        @NotNull @PositiveOrZero Double value
) {
}
