package com.autotea.backend.dto;

import com.autotea.backend.domain.EquipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FormulaTemplateRequest(
        @NotNull EquipmentType equipmentType,
        @NotBlank String name,
        @NotNull Double k1,
        @NotNull Double k2,
        @NotNull Double k3
) {
}
