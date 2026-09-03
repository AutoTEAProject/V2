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

    /** 업로드된 파일(input.xlsx/input.rep) 묶음의 이름. 같은 파일로 설정만 바꿔 재실행할 때는 바뀌지 않는다. */
    @Column(length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.DRAFT;

    @Column(name = "input_xlsx_name")
    private String inputXlsxName;

    @Column(name = "input_rep_name")
    private String inputRepName;

    /**
     * 업로드된 input.xlsx/input.rep 원본 바이트. 재실행(같은 파일, 다른 설정) 시 다시 업로드받지 않고
     * 이걸 그대로 python-engine에 재전송하기 위해 보관한다(서비스 간 공유 디스크가 없는 배포 환경 전제).
     */
    @Column(name = "input_xlsx_data", columnDefinition = "bytea")
    private byte[] inputXlsxData;

    @Column(name = "input_rep_data", columnDefinition = "bytea")
    private byte[] inputRepData;

    @Column(name = "result_data", columnDefinition = "bytea")
    private byte[] resultData;

    /** 장치 이름 -> (수식 이름 -> 계산된 값들) JSON. SUCCESS run에서만 채워짐. */
    @Column(name = "cost_result", columnDefinition = "TEXT")
    private String costResult;

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

    public CalculationRun(TeaCase teaCase, String name, String inputXlsxName, String inputRepName) {
        this.teaCase = teaCase;
        this.name = name;
        this.inputXlsxName = inputXlsxName;
        this.inputRepName = inputRepName;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
