package com.autotea.backend.service;

import com.autotea.backend.domain.EquipmentFormulaTemplate;
import com.autotea.backend.domain.EquipmentType;
import com.autotea.backend.domain.ProjectEquipmentSetting;
import com.autotea.backend.domain.TeaCase;
import com.autotea.backend.domain.UtilityType;
import com.autotea.backend.dto.EquipmentInstanceResponse;
import com.autotea.backend.dto.EquipmentSettingRequest;
import com.autotea.backend.dto.EquipmentSettingResponse;
import com.autotea.backend.dto.FormulaTemplateResponse;
import com.autotea.backend.repository.EquipmentFormulaTemplateRepository;
import com.autotea.backend.repository.ProjectEquipmentSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 케이스(프로젝트)의 장치비/유틸리티 설정을 다룬다.
 * 조회/실행 시 우선순위는: 인스턴스 오버라이드 > 타입 기본값(instanceName="*") > 시스템 시드 수식 기본값.
 */
@Service
@RequiredArgsConstructor
public class EquipmentSettingService {

    private final ProjectEquipmentSettingRepository settingRepository;
    private final EquipmentFormulaTemplateRepository formulaRepository;
    private final FormulaService formulaService;
    private final CaseService caseService;

    public List<EquipmentSettingResponse> findAllForCase(Long caseId) {
        caseService.getOrThrow(caseId);
        List<ProjectEquipmentSetting> existing = settingRepository.findAllByTeaCaseId(caseId);
        Map<EquipmentType, ProjectEquipmentSetting> typeDefaults = existing.stream()
                .filter(ProjectEquipmentSetting::isTypeDefault)
                .collect(Collectors.toMap(ProjectEquipmentSetting::getEquipmentType, s -> s));

        List<EquipmentSettingResponse> result = new ArrayList<>();
        for (EquipmentType type : EquipmentType.values()) {
            ProjectEquipmentSetting setting = typeDefaults.get(type);
            result.add(setting != null
                    ? EquipmentSettingResponse.from(setting)
                    : syntheticTypeDefault(type));
        }
        existing.stream()
                .filter(s -> !s.isTypeDefault())
                .map(EquipmentSettingResponse::from)
                .forEach(result::add);
        return result;
    }

    @Transactional
    public void upsert(Long caseId, EquipmentSettingRequest request) {
        TeaCase teaCase = caseService.getOrThrow(caseId);
        for (EquipmentSettingRequest.Item item : request.items()) {
            String instanceName = normalizeInstanceName(item.instanceName());
            ProjectEquipmentSetting setting = settingRepository
                    .findByTeaCaseIdAndEquipmentTypeAndInstanceName(caseId, item.equipmentType(), instanceName)
                    .orElseGet(() -> new ProjectEquipmentSetting(teaCase, item.equipmentType(), instanceName));

            setting.setSkipCost(item.skipCost());
            setting.setDefaultFormula(item.defaultFormulaTemplateId() == null
                    ? null
                    : formulaService.getOrThrow(item.defaultFormulaTemplateId()));

            Set<EquipmentFormulaTemplate> selected = item.selectedFormulaTemplateIds() == null
                    ? Set.of()
                    : item.selectedFormulaTemplateIds().stream()
                        .map(formulaService::getOrThrow)
                        .collect(Collectors.toSet());
            setting.setSelectedFormulas(selected);

            setting.setUtilityTypes(item.utilityTypes() == null ? Set.of() : new HashSet<>(item.utilityTypes()));

            settingRepository.save(setting);
        }
    }

    /**
     * 발견된 장치 인스턴스 목록(python-engine parse 결과)을 바탕으로, 실행 시 engine에 넘길 설정 JSON을 만든다.
     * HTX/HEX/COMP/REACT가 아닌 장치(MIX, FLASH 등)는 애초에 원가 계산 대상이 아니므로 제외한다.
     */
    public Map<String, Object> buildEngineConfig(Long caseId, List<EquipmentInstanceResponse> instances) {
        Map<String, Object> equipmentMap = new LinkedHashMap<>();
        Map<EquipmentType, Map<String, Object>> coefficients = new EnumMap<>(EquipmentType.class);

        for (EquipmentInstanceResponse instance : instances) {
            EquipmentType type = parseType(instance.type());
            if (type == null) {
                continue;
            }
            Effective effective = resolveEffective(caseId, type, instance.name());

            Map<String, Object> equipmentEntry = new LinkedHashMap<>();
            equipmentEntry.put("type", type.name());
            equipmentEntry.put("skipCost", effective.skipCost());
            equipmentEntry.put("defaultFormula", effective.defaultFormula() == null ? null : effective.defaultFormula().getName());
            equipmentEntry.put("selectedFormulas", effective.selectedFormulas().stream().map(EquipmentFormulaTemplate::getName).toList());
            equipmentEntry.put("utilityTypes", effective.utilityTypes().stream().map(Enum::name).toList());
            equipmentMap.put(instance.name(), equipmentEntry);

            if (type != EquipmentType.REACT) {
                Map<String, Object> typeCoefficients = coefficients.computeIfAbsent(type, t -> new LinkedHashMap<>());
                for (EquipmentFormulaTemplate template : effective.selectedFormulas()) {
                    typeCoefficients.putIfAbsent(template.getName(),
                            Map.of("K1", template.getK1(), "K2", template.getK2(), "K3", template.getK3()));
                }
            }
        }

        Map<String, Object> formulaCoefficients = new LinkedHashMap<>();
        coefficients.forEach((type, map) -> formulaCoefficients.put(type.name(), map));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("equipment", equipmentMap);
        config.put("formulaCoefficients", formulaCoefficients);
        return config;
    }

    private Effective resolveEffective(Long caseId, EquipmentType type, String instanceName) {
        return settingRepository.findByTeaCaseIdAndEquipmentTypeAndInstanceName(caseId, type, instanceName)
                .map(Effective::fromEntity)
                .or(() -> settingRepository
                        .findByTeaCaseIdAndEquipmentTypeAndInstanceName(caseId, type, ProjectEquipmentSetting.TYPE_DEFAULT)
                        .map(Effective::fromEntity))
                .orElseGet(() -> Effective.systemDefault(type, formulaRepository.findAllByEquipmentTypeAndSystemDefaultTrueOrderById(type)));
    }

    private EquipmentSettingResponse syntheticTypeDefault(EquipmentType type) {
        List<EquipmentFormulaTemplate> systemTemplates = formulaRepository.findAllByEquipmentTypeAndSystemDefaultTrueOrderById(type);
        FormulaTemplateResponse defaultFormula = systemTemplates.isEmpty() ? null : FormulaTemplateResponse.from(systemTemplates.get(0));
        List<FormulaTemplateResponse> selected = systemTemplates.stream().map(FormulaTemplateResponse::from).toList();
        List<UtilityType> utilityTypes = type == EquipmentType.REACT ? List.of() : List.of(UtilityType.values());
        return new EquipmentSettingResponse(type, ProjectEquipmentSetting.TYPE_DEFAULT, true, false, defaultFormula, selected, utilityTypes);
    }

    private String normalizeInstanceName(String instanceName) {
        return (instanceName == null || instanceName.isBlank()) ? ProjectEquipmentSetting.TYPE_DEFAULT : instanceName;
    }

    private EquipmentType parseType(String rawType) {
        try {
            return EquipmentType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record Effective(
            boolean skipCost,
            EquipmentFormulaTemplate defaultFormula,
            Set<EquipmentFormulaTemplate> selectedFormulas,
            Set<UtilityType> utilityTypes
    ) {
        static Effective fromEntity(ProjectEquipmentSetting s) {
            return new Effective(s.isSkipCost(), s.getDefaultFormula(), s.getSelectedFormulas(), s.getUtilityTypes());
        }

        static Effective systemDefault(EquipmentType type, List<EquipmentFormulaTemplate> systemTemplates) {
            EquipmentFormulaTemplate def = systemTemplates.isEmpty() ? null : systemTemplates.get(0);
            Set<UtilityType> utilities = type == EquipmentType.REACT ? Set.of() : Set.of(UtilityType.values());
            return new Effective(false, def, new HashSet<>(systemTemplates), utilities);
        }
    }
}
