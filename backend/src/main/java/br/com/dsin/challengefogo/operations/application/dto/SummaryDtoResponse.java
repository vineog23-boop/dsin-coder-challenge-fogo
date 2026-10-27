package br.com.dsin.challengefogo.operations.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Indicadores da central. Toda situação e toda região aparecem, mesmo com zero, para o painel não "piscar". */
public record SummaryDtoResponse(EquipmentSummary equipments, AgentSummary agents, List<RegionSummary> regions,
                                 Instant generatedAt) {

    public record EquipmentSummary(long total, Map<EquipmentStatus, Long> byStatus, long immediateMaintenance,
                                   long overdueMaintenance) {
    }

    /** {@code active} = disponíveis + em ocorrência (regra 4). */
    public record AgentSummary(long total, long active, Map<AgentStatus, Long> byStatus) {
    }

    public record RegionSummary(Region region, long equipmentsInMalfunction, long activeAgents) {
    }
}
