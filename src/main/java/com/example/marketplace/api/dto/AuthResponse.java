package com.example.marketplace.api.dto;

import com.example.marketplace.domain.enums.UserRole;

public record AuthResponse(
        String token,
        Long userId,
        String nome,
        String email,
        String whatsapp,
        String avatarUrl,
        UserRole role
) {
}
