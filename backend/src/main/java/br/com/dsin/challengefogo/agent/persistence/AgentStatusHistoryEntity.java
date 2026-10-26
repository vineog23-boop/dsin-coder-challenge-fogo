package br.com.dsin.challengefogo.agent.persistence;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma mudança de situação. Guarda só o id do agente (sem relacionamento JPA): o histórico
 * é lido por consulta própria e nunca navegado a partir do agente.
 */
@Entity
@Table(name = "agent_status_history")
public class AgentStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private AgentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20, updatable = false)
    private AgentStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
    private String changedBy;

    @Column(length = 500, updatable = false)
    private String note;

    protected AgentStatusHistoryEntity() {
    }

    public AgentStatusHistoryEntity(UUID agentId, AgentStatus fromStatus, AgentStatus toStatus,
                                        Instant changedAt, String changedBy, String note) {
        this.agentId = agentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public AgentStatus getFromStatus() {
        return fromStatus;
    }

    public AgentStatus getToStatus() {
        return toStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getNote() {
        return note;
    }
}
