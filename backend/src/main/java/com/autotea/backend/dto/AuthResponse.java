package com.autotea.backend.dto;

import com.autotea.backend.domain.User;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String displayName,
        String pictureUrl
) {
    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getPictureUrl());
    }
}
