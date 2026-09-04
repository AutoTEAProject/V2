package com.autotea.backend.dto;

import com.autotea.backend.domain.StreamDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StreamSettingItem(
        @NotBlank String streamName,
        @NotNull StreamDirection direction,
        /** USD/kg. direction=IN일 때만 사용. */
        Double cost
) {
}
