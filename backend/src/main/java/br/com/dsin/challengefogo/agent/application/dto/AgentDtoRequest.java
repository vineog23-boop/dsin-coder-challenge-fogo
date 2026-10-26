package br.com.dsin.challengefogo.agent.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Criação e edição. O órgão não vem no corpo: é derivado do tipo. {@code status} vale só na criação
 * (padrão {@code AVAILABLE}); depois muda pelo {@code PATCH /status}, que grava histórico.
 */
public record AgentDtoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "A matrícula é obrigatória.")
        @Size(max = 30, message = "A matrícula deve ter no máximo 30 caracteres.")
        String registrationNumber,

        @NotNull(message = "O tipo de agente é obrigatório.")
        AgentType agentType,

        AgentStatus status,

        @NotNull(message = "A base é obrigatória.")
        @Valid
        LocationDto baseLocation,

        UUID vehicleId) {
}
