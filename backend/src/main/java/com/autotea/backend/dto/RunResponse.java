package com.autotea.backend.dto;

import com.autotea.backend.domain.CalculationRun;
import com.autotea.backend.domain.RunStatus;

import java.time.Instant;

public record RunResponse(
        Long id,
        Long caseId,
        String name,
        RunStatus status,
        String inputXlsxName,
        String inputRepName,
        /** [{"name":"...","type":"HEX"}, ...] 형태의 원문 JSON. parse 단계 완료 후 채워짐. */
        String equipmentSnapshot,
        String errorMessage,
        String logs,
        Instant createdAt,
        Instant updatedAt
) {
    public static RunResponse from(CalculationRun run) {
        return new RunResponse(
                run.getId(),
                run.getTeaCase().getId(),
                run.getName(),
                run.getStatus(),
                run.getInputXlsxName(),
                run.getInputRepName(),
                run.getEquipmentSnapshot(),
                run.getErrorMessage(),
                run.getLogs(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }
}
