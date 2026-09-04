package com.autotea.backend.dto;

import com.autotea.backend.domain.CostSource;
import com.autotea.backend.domain.EquipmentType;
import com.autotea.backend.domain.ProjectEquipmentSetting;
import com.autotea.backend.domain.UtilityType;

import java.util.List;

public record EquipmentSettingResponse(
        EquipmentType equipmentType,
        String instanceName,
        boolean typeDefault,
        boolean skipCost,
        CostSource costSource,
        FormulaTemplateResponse defaultFormula,
        List<FormulaTemplateResponse> selectedFormulas,
        List<UtilityType> utilityTypes
) {
    public static EquipmentSettingResponse from(ProjectEquipmentSetting setting) {
        return new EquipmentSettingResponse(
                setting.getEquipmentType(),
                setting.getInstanceName(),
                setting.isTypeDefault(),
                setting.isSkipCost(),
                setting.getCostSource(),
                setting.getDefaultFormula() == null ? null : FormulaTemplateResponse.from(setting.getDefaultFormula()),
                setting.getSelectedFormulas().stream().map(FormulaTemplateResponse::from).toList(),
                setting.getUtilityTypes().stream().toList()
        );
    }
}
