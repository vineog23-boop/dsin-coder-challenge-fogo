package br.com.dsin.challengefogo.agent.persistence;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Agente em campo. Guarda só dados funcionais (sem CPF, telefone ou endereço residencial).
 * A viatura é um {@link VehicleEntity} do cadastro de equipamentos; mais de um agente pode dividi-la.
 */
@Entity
@Table(name = "agent")
public class AgentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 20)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status;

    @Embedded
    private Location baseLocation;

    @Column(name = "current_latitude", precision = 9, scale = 6)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 9, scale = 6)
    private BigDecimal currentLongitude;

    @Column(name = "position_updated_at")
    private Instant positionUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentEntity() {
    }

    /** O órgão sai do tipo: não existe guarda municipal da Polícia Militar. */
    public AgentEntity(String name, String registrationNumber, AgentType agentType, AgentStatus status,
                       Location baseLocation, VehicleEntity vehicle) {
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.agentType = agentType;
        this.agency = agentType.agency();
        this.status = status;
        this.baseLocation = baseLocation;
        this.vehicle = vehicle;
    }

    /** Edita os dados cadastrais; a situação tem rota própria, que grava histórico. */
    public void update(String name, String registrationNumber, AgentType agentType, Location baseLocation,
                       VehicleEntity vehicle) {
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.agentType = agentType;
        this.agency = agentType.agency();
        this.baseLocation = baseLocation;
        this.vehicle = vehicle;
    }

    /** Muda a situação e devolve a anterior. Sair de serviço desvincula a viatura (regra 5). */
    public AgentStatus changeStatus(AgentStatus newStatus) {
        if (newStatus == status) {
            throw new BusinessRuleException("O agente já está nessa situação.");
        }
        AgentStatus previous = status;
        status = newStatus;
        if (!newStatus.isOnDuty()) {
            vehicle = null;
        }
        return previous;
    }

    public void updatePosition(BigDecimal latitude, BigDecimal longitude, Instant at) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.positionUpdatedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public Agency getAgency() {
        return agency;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public BigDecimal getCurrentLatitude() {
        return currentLatitude;
    }

    public BigDecimal getCurrentLongitude() {
        return currentLongitude;
    }

    public Instant getPositionUpdatedAt() {
        return positionUpdatedAt;
    }

    public VehicleEntity getVehicle() {
        return vehicle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
