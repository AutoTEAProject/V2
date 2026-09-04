package com.autotea.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 케이스(프로젝트)의 장치비/유틸리티 설정 한 건.
 * instanceName == TYPE_DEFAULT("*") 이면 장치 타입 전체의 기본값,
 * 그 외에는 파싱으로 발견된 실제 장치 이름에 대한 개별 오버라이드.
 */
@Entity
@Table(name = "project_equipment_setting")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEquipmentSetting {

    public static final String TYPE_DEFAULT = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private TeaCase teaCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false, length = 20)
    private EquipmentType equipmentType;

    @Column(name = "instance_name", nullable = false, length = 200)
    private String instanceName = TYPE_DEFAULT;

    @Column(name = "skip_cost", nullable = false)
    private boolean skipCost = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_source", nullable = false, length = 20)
    private CostSource costSource = CostSource.FORMULA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_formula_template_id")
    private EquipmentFormulaTemplate defaultFormula;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "project_equipment_selected_formula",
            joinColumns = @JoinColumn(name = "setting_id"),
            inverseJoinColumns = @JoinColumn(name = "formula_template_id")
    )
    private Set<EquipmentFormulaTemplate> selectedFormulas = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_equipment_utility_type", joinColumns = @JoinColumn(name = "setting_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type")
    private Set<UtilityType> utilityTypes = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProjectEquipmentSetting(TeaCase teaCase, EquipmentType equipmentType, String instanceName) {
        this.teaCase = teaCase;
        this.equipmentType = equipmentType;
        this.instanceName = instanceName;
    }

    public boolean isTypeDefault() {
        return TYPE_DEFAULT.equals(instanceName);
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
