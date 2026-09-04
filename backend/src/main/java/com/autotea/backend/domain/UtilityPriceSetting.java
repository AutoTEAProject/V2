package com.autotea.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * utility 타입별 현재 단가. 케이스/run과 무관한 전역 설정값(수식 라이브러리와 같은 성격).
 * python-engine 도커 이미지에 고정돼 있던 값을 여기로 옮겨와 자유롭게 수정할 수 있게 한 것.
 */
@Entity
@Table(name = "utility_price_setting")
@Getter
@Setter
@NoArgsConstructor
public class UtilityPriceSetting {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", length = 20)
    private UtilityType utilityType;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UtilityPriceSetting(UtilityType utilityType, Double value, String unit) {
        this.utilityType = utilityType;
        this.value = value;
        this.unit = unit;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
