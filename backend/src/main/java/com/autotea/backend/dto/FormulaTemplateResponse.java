package com.autotea.backend.dto;

import com.autotea.backend.domain.EquipmentFormulaTemplate;
import com.autotea.backend.domain.EquipmentType;

public record FormulaTemplateResponse(
        Long id,
        EquipmentType equipmentType,
        String name,
        Double k1,
        Double k2,
        Double k3,
        boolean systemDefault
) {
    public static FormulaTemplateResponse from(EquipmentFormulaTemplate template) {
        return new FormulaTemplateResponse(
                template.getId(),
                template.getEquipmentType(),
                template.getName(),
                template.getK1(),
                template.getK2(),
                template.getK3(),
                template.isSystemDefault()
        );
    }
}
