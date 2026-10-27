package br.com.dsin.challengefogo.agent.persistence;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Dados do marcador do agente; posição atual e base vêm juntas para a escolha da regra 1. */
public record AgentMarkerView(UUID id, String name, AgentType agentType, AgentStatus status,
                              BigDecimal currentLatitude, BigDecimal currentLongitude, Instant positionUpdatedAt,
                              BigDecimal baseLatitude, BigDecimal baseLongitude, Region region,
                              String vehiclePlate) {
}
