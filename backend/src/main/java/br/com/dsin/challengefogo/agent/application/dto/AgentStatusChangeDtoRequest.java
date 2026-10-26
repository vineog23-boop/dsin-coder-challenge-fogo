package br.com.dsin.challengefogo.agent.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentStatusChangeDtoRequest(
        @NotNull(message = "A nova situação é obrigatória.")
        AgentStatus status,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String note) {
}
