package com.autotea.backend.dto;

import jakarta.validation.Valid;

import java.util.List;

public record StreamSettingRequest(
        @Valid List<StreamSettingItem> items
) {
}
