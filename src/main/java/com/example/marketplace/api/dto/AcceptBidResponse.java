package com.example.marketplace.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcceptBidResponse(
        Long saleId,
        Long listingId,
        Long acceptedBidId,
        String buyerName,
        String buyerAvatarUrl,
        BigDecimal acceptedAmount,
        String whatsappLink,
        String whatsappMessage,
        LocalDateTime closingDeadline
) {
}
