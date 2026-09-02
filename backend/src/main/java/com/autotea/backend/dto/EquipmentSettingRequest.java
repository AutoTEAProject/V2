package com.autotea.backend.dto;

import com.autotea.backend.domain.EquipmentType;
import com.autotea.backend.domain.UtilityType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

public record EquipmentSettingRequest(
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotNull EquipmentType equipmentType,
            /** null 또는 "*" 이면 장치 타입 기본값, 그 외에는 개별 장치 이름 오버라이드 */
            String instanceName,
            boolean skipCost,
            Long defaultFormulaTemplateId,
            List<Long> selectedFormulaTemplateIds,
            List<UtilityType> utilityTypes
    ) {
    }
}
