package br.com.dsin.challengefogo.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDtoRequest(
        @NotBlank(message = "O usuário é obrigatório.")
        String username,

        @NotBlank(message = "A senha é obrigatória.")
        String password) {
}
