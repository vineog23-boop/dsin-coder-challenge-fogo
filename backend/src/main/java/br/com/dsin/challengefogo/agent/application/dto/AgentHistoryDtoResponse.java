package br.com.dsin.challengefogo.agent.application.dto;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusHistoryEntity;

import java.time.Instant;

public record AgentHistoryDtoResponse(AgentStatus fromStatus, AgentStatus toStatus, Instant changedAt,
                                      String changedBy, String note) {

    public AgentHistoryDtoResponse(AgentStatusHistoryEntity change) {
        this(change.getFromStatus(), change.getToStatus(), change.getChangedAt(), change.getChangedBy(),
                change.getNote());
    }
}
