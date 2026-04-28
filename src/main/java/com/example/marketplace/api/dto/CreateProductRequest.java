package com.example.marketplace.api.dto;

import com.example.marketplace.domain.enums.ProductCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank String nome,
        @NotBlank String descricao,
        @NotBlank String categoria,
        @NotNull ProductCondition condicao,
        String imageUrl
) {
}
