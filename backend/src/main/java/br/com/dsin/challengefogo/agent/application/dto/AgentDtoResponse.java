package br.com.dsin.challengefogo.agent.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.domain.Agency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentDtoResponse(
        UUID id,
        String name,
        String registrationNumber,
        AgentType agentType,
        Agency agency,
        AgentStatus status,
        LocationDto baseLocation,
        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        Instant positionUpdatedAt,
        VehicleSummaryDto vehicle,
        Instant createdAt,
        Instant updatedAt) {

    public AgentDtoResponse(AgentEntity agent) {
        this(agent.getId(), agent.getName(), agent.getRegistrationNumber(), agent.getAgentType(), agent.getAgency(),
                agent.getStatus(), agent.getBaseLocation() != null ? new LocationDto(agent.getBaseLocation()) : null,
                agent.getCurrentLatitude(), agent.getCurrentLongitude(), agent.getPositionUpdatedAt(),
                agent.getVehicle() != null ? new VehicleSummaryDto(agent.getVehicle()) : null,
                agent.getCreatedAt(), agent.getUpdatedAt());
    }
}
