package com.example.marketplace.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotBlank @Pattern(
                regexp = "^\\d{10,15}$",
                message = "WhatsApp deve conter de 10 a 15 dígitos"
        ) String whatsapp,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}