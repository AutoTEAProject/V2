package com.autotea.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description
) {
}
