package com.autotea.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "calculation_run")
@Getter
@Setter
@NoArgsConstructor
public class CalculationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private TeaCase teaCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.DRAFT;

    @Column(name = "input_xlsx_name")
    private String inputXlsxName;

    @Column(name = "input_rep_name")
    private String inputRepName;

    @Column(name = "result_path", length = 500)
    private String resultPath;

    /** parse 단계에서 발견된 장치 목록(JSON: [{"name":..,"type":..}]) 스냅샷. */
    @Column(name = "equipment_snapshot", columnDefinition = "TEXT")
    private String equipmentSnapshot;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String logs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CalculationRun(TeaCase teaCase, String inputXlsxName, String inputRepName) {
        this.teaCase = teaCase;
        this.inputXlsxName = inputXlsxName;
        this.inputRepName = inputRepName;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
