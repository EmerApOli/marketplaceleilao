package com.example.marketplace.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleConfirmationResponse(
        Long saleId,
        String status,
        BigDecimal finalAmount,
        BigDecimal adminCommission,
        LocalDateTime completedAt,
        boolean adminNotified,
        boolean platformPaid,
        LocalDateTime platformPaidAt
) {
}
