package br.com.dsin.challengefogo.operations.application.dto;

import java.util.List;

/** Pontos do mapa. {@code truncated} avisa que alguma camada bateu no teto e há pontos fora da resposta. */
public record MapDtoResponse(List<EquipmentMarkerDto> equipments, List<AgentMarkerDto> agents, boolean truncated) {
}
