package com.autotea.backend.repository;

import com.autotea.backend.domain.EquipmentFormulaTemplate;
import com.autotea.backend.domain.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentFormulaTemplateRepository extends JpaRepository<EquipmentFormulaTemplate, Long> {
    List<EquipmentFormulaTemplate> findAllByEquipmentType(EquipmentType equipmentType);

    List<EquipmentFormulaTemplate> findAllByEquipmentTypeAndSystemDefaultTrueOrderById(EquipmentType equipmentType);
}
