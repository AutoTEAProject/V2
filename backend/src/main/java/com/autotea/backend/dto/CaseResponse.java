package com.autotea.backend.dto;

import com.autotea.backend.domain.TeaCase;

import java.time.Instant;

public record CaseResponse(
        Long id,
        String name,
        String description,
        double plantOperationHours,
        double depreciationLifetime,
        Instant createdAt
) {
    public static CaseResponse from(TeaCase teaCase) {
        return new CaseResponse(
                teaCase.getId(),
                teaCase.getName(),
                teaCase.getDescription(),
                teaCase.getPlantOperationHours(),
                teaCase.getDepreciationLifetime(),
                teaCase.getCreatedAt()
        );
    }
}
