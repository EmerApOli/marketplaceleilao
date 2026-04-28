package com.example.marketplace.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateBidRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
