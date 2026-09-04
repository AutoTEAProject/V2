package com.autotea.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tea_case")
@Getter
@Setter
@NoArgsConstructor
public class TeaCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    /** 연간 플랜트 가동 시간 [hours/year]. utility 연간 사용량/비용, 생산량 계산에 쓰인다. */
    @Column(name = "plant_operation_hours", nullable = false)
    private double plantOperationHours = 8766;

    /** 감가상각 내용연수 [years]. Profitability Analysis의 Depreciation 계산에 쓰인다. */
    @Column(name = "depreciation_lifetime", nullable = false)
    private double depreciationLifetime = 30;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TeaCase(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
