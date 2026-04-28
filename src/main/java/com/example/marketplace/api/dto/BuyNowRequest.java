package com.example.marketplace.api.dto;

import jakarta.validation.constraints.NotNull;

public record BuyNowRequest(
        @NotNull Long buyerId
) {
}
