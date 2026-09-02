package com.autotea.backend.repository;

import com.autotea.backend.domain.TeaCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeaCaseRepository extends JpaRepository<TeaCase, Long> {
}
