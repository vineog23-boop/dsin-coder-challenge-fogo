package br.com.dsin.challengefogo.agent.persistence;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;

public record AgentStatusCount(AgentStatus status, long total) {
}
