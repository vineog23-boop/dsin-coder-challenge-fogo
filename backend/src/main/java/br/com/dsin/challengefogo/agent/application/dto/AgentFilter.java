package br.com.dsin.challengefogo.agent.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;

/** Filtros da listagem; {@code region} é a da base e {@code search} busca em nome e matrícula. */
public record AgentFilter(AgentType agentType, AgentStatus status, Agency agency, Region region, String search) {
}
