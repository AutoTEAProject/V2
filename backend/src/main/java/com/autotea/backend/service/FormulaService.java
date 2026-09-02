package com.autotea.backend.service;

import com.autotea.backend.domain.EquipmentFormulaTemplate;
import com.autotea.backend.domain.EquipmentType;
import com.autotea.backend.dto.FormulaTemplateRequest;
import com.autotea.backend.exception.FormulaInUseException;
import com.autotea.backend.exception.ResourceNotFoundException;
import com.autotea.backend.repository.EquipmentFormulaTemplateRepository;
import com.autotea.backend.repository.ProjectEquipmentSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormulaService {

    private final EquipmentFormulaTemplateRepository formulaRepository;
    private final ProjectEquipmentSettingRepository equipmentSettingRepository;

    public List<EquipmentFormulaTemplate> findAll(EquipmentType equipmentType) {
        if (equipmentType == null) {
            return formulaRepository.findAll();
        }
        return formulaRepository.findAllByEquipmentType(equipmentType);
    }

    public EquipmentFormulaTemplate create(FormulaTemplateRequest request) {
        EquipmentFormulaTemplate template = new EquipmentFormulaTemplate(
                request.equipmentType(), request.name(), request.k1(), request.k2(), request.k3());
        return formulaRepository.save(template);
    }

    public EquipmentFormulaTemplate update(Long id, FormulaTemplateRequest request) {
        EquipmentFormulaTemplate template = getOrThrow(id);
        template.setEquipmentType(request.equipmentType());
        template.setName(request.name());
        template.setK1(request.k1());
        template.setK2(request.k2());
        template.setK3(request.k3());
        return formulaRepository.save(template);
    }

    public void delete(Long id) {
        getOrThrow(id);
        boolean inUse = equipmentSettingRepository.existsByDefaultFormulaId(id)
                || equipmentSettingRepository.existsBySelectedFormulas_Id(id);
        if (inUse) {
            throw new FormulaInUseException("이 수식은 하나 이상의 프로젝트 설정에서 사용 중이라 삭제할 수 없습니다.");
        }
        formulaRepository.deleteById(id);
    }

    public EquipmentFormulaTemplate getOrThrow(Long id) {
        return formulaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formula template not found: " + id));
    }
}
