package br.com.dsin.challengefogo.agent.application;

import br.com.dsin.challengefogo.shared.domain.NotFoundException;

import java.util.UUID;

public class AgentNotFoundException extends NotFoundException {

    public AgentNotFoundException(UUID id) {
        super("Agente " + id + " não encontrado.");
    }
}
