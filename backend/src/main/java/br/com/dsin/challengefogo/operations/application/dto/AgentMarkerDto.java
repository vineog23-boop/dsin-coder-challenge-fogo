package br.com.dsin.challengefogo.operations.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentMarkerView;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Marcador do agente; {@code positionSource} diz se o ponto é a posição em campo ou a base (regra 1). */
public record AgentMarkerDto(UUID id, String name, AgentType agentType, AgentStatus status, BigDecimal latitude,
                             BigDecimal longitude, PositionSource positionSource, Instant positionUpdatedAt,
                             String vehiclePlate, Region region) {

    public enum PositionSource {
        CURRENT,
        BASE
    }

    public AgentMarkerDto(AgentMarkerView view) {
        this(view.id(), view.name(), view.agentType(), view.status(),
                hasCurrent(view) ? view.currentLatitude() : view.baseLatitude(),
                hasCurrent(view) ? view.currentLongitude() : view.baseLongitude(),
                hasCurrent(view) ? PositionSource.CURRENT : PositionSource.BASE,
                hasCurrent(view) ? view.positionUpdatedAt() : null,
                view.vehiclePlate(), view.region());
    }

    private static boolean hasCurrent(AgentMarkerView view) {
        return view.currentLatitude() != null && view.currentLongitude() != null;
    }
}
