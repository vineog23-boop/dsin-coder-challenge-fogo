package br.com.dsin.challengefogo.operations.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;

/**
 * Filtros do mapa, vindos da query string. {@code region}, {@code agency} e {@code neighborhood} valem para as
 * duas camadas (no agente, o bairro é o da base); sem {@code agentStatus}, o mapa mostra só os agentes em
 * atividade (regra 2).
 */
public record MapFilter(EquipmentType equipmentType, EquipmentStatus equipmentStatus, AgentStatus agentStatus,
                        Region region, Agency agency, String neighborhood) {
}
