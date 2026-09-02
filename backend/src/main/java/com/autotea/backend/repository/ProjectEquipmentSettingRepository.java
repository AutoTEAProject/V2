package com.autotea.backend.repository;

import com.autotea.backend.domain.EquipmentType;
import com.autotea.backend.domain.ProjectEquipmentSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectEquipmentSettingRepository extends JpaRepository<ProjectEquipmentSetting, Long> {
    List<ProjectEquipmentSetting> findAllByTeaCaseId(Long caseId);

    Optional<ProjectEquipmentSetting> findByTeaCaseIdAndEquipmentTypeAndInstanceName(
            Long caseId, EquipmentType equipmentType, String instanceName);

    boolean existsByDefaultFormulaId(Long formulaTemplateId);

    boolean existsBySelectedFormulas_Id(Long formulaTemplateId);
}
