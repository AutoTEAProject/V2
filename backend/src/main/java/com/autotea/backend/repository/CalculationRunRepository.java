package com.autotea.backend.repository;

import com.autotea.backend.domain.CalculationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculationRunRepository extends JpaRepository<CalculationRun, Long> {
    List<CalculationRun> findByTeaCaseIdOrderByCreatedAtDesc(Long caseId);
}
