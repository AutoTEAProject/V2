package com.autotea.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "equipment_formula_template")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentFormulaTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false, length = 20)
    private EquipmentType equipmentType;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Double k1;

    @Column(nullable = false)
    private Double k2;

    @Column(nullable = false)
    private Double k3;

    @Column(name = "is_system_default", nullable = false)
    private boolean systemDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EquipmentFormulaTemplate(EquipmentType equipmentType, String name, Double k1, Double k2, Double k3) {
        this.equipmentType = equipmentType;
        this.name = name;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
