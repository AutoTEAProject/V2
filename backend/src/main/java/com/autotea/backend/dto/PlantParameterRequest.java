package com.autotea.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlantParameterRequest(
        @NotNull @Positive Double plantOperationHours,
        @NotNull @Positive Double depreciationLifetime
) {
}
