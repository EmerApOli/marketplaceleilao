package com.example.marketplace.api.dto;

import java.math.BigDecimal;

public record AdminDashboardResponse(
        BigDecimal totalSalesAmount,
        BigDecimal totalCommissionAmount,
        BigDecimal totalCompletedCommissionAmount,
        BigDecimal totalPendingCommissionAmount,
        BigDecimal totalPaidCommissionAmount,
        long totalSales,
        long completedSales,
        long pendingSales,
        long expiredSales,
        long sellerPendingPayments
) {
}
