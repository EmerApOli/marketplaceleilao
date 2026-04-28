package com.example.marketplace.api.dto;

import java.time.LocalDateTime;

public record ContactInfoResponse(
        String counterpartName,
        String maskedWhatsapp,
        String whatsappLink,
        String context,
        LocalDateTime availableUntil
) {
}
