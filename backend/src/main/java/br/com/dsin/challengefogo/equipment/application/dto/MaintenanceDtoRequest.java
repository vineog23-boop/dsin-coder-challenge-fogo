package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record MaintenanceDtoRequest(
        @NotNull(message = "O tipo de manutenção é obrigatório.")
        MaintenanceKind kind,

        @NotNull(message = "A data da manutenção é obrigatória.")
        @PastOrPresent(message = "A data da manutenção não pode estar no futuro.")
        Instant performedAt,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description) {
}
