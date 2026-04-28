package com.example.marketplace.api.dto;

import com.example.marketplace.domain.enums.ProductCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateListingRequest(
        @NotBlank String nome,
        @NotBlank String descricao,
        String categoria,
        @NotNull ProductCondition condicao,
        @NotNull @DecimalMin(value = "0.01") BigDecimal initialPrice,
        @NotNull @DecimalMin(value = "0.01") BigDecimal buyNowPrice,
        @NotNull Integer durationInDays,
        Long listingId
) {
}
