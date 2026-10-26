package br.com.dsin.challengefogo.agent.application;

import br.com.dsin.challengefogo.agent.application.dto.AgentDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.AgentDtoResponse;
import br.com.dsin.challengefogo.agent.application.dto.AgentFilter;
import br.com.dsin.challengefogo.agent.application.dto.AgentHistoryDtoResponse;
import br.com.dsin.challengefogo.agent.application.dto.AgentStatusChangeDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.PositionDtoRequest;
import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusHistoryEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AgentService {

    private static final Set<EquipmentType> PATROL_VEHICLE_TYPES = EnumSet.of(EquipmentType.TRAFFIC_AGENT_MOTORCYCLE,
            EquipmentType.MUNICIPAL_GUARD_VEHICLE, EquipmentType.MILITARY_POLICE_VEHICLE);
    private static final Set<EquipmentStatus> UNUSABLE_VEHICLE_STATUSES =
            EnumSet.of(EquipmentStatus.DEACTIVATED, EquipmentStatus.STOLEN);

    private final AgentRepository agentRepository;
    private final AgentStatusHistoryRepository statusHistoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final Clock clock;

    public AgentService(AgentRepository agentRepository, AgentStatusHistoryRepository statusHistoryRepository,
                        EquipmentRepository equipmentRepository, Clock clock) {
        this.agentRepository = agentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.equipmentRepository = equipmentRepository;
        this.clock = clock;
    }

    @Transactional
    public AgentDtoResponse create(AgentDtoRequest request) {
        AgentStatus status = request.status() != null ? request.status() : AgentStatus.AVAILABLE;
        if (agentRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw duplicatedRegistration(request.registrationNumber());
        }
        VehicleEntity vehicle = compatibleVehicle(request.vehicleId(), request.agentType(), status);

        AgentEntity agent = agentRepository.save(new AgentEntity(request.name(), request.registrationNumber(),
                request.agentType(), status, request.baseLocation().toLocation(), vehicle));

        return new AgentDtoResponse(agent);
    }

    @Transactional(readOnly = true)
    public AgentDtoResponse findById(UUID id) {
        return new AgentDtoResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<AgentDtoResponse> search(AgentFilter filter, Pageable pageable) {
        return agentRepository.findAll(AgentSpecifications.from(filter), pageable).map(AgentDtoResponse::new);
    }

    @Transactional
    public AgentDtoResponse update(UUID id, AgentDtoRequest request) {
        AgentEntity agent = find(id);
        if (request.status() != null && request.status() != agent.getStatus()) {
            throw new BusinessRuleException("A situação muda pela rota própria, que grava o histórico.");
        }
        if (agentRepository.existsByRegistrationNumberAndIdNot(request.registrationNumber(), id)) {
            throw duplicatedRegistration(request.registrationNumber());
        }
        VehicleEntity vehicle = compatibleVehicle(request.vehicleId(), request.agentType(), agent.getStatus());

        agent.update(request.name(), request.registrationNumber(), request.agentType(),
                request.baseLocation().toLocation(), vehicle);
        // flush para o updatedAt da resposta já refletir a alteração
        agentRepository.saveAndFlush(agent);

        return new AgentDtoResponse(agent);
    }

    @Transactional
    public AgentDtoResponse changeStatus(UUID id, AgentStatusChangeDtoRequest request, String username) {
        AgentEntity agent = find(id);

        AgentStatus previous = agent.changeStatus(request.status());
        statusHistoryRepository.save(new AgentStatusHistoryEntity(id, previous, request.status(), clock.instant(),
                username, request.note()));
        agentRepository.saveAndFlush(agent);

        return new AgentDtoResponse(agent);
    }

    /** Liberado também ao operador: é a "telemetria" do agente em campo. Não grava histórico. */
    @Transactional
    public AgentDtoResponse updatePosition(UUID id, PositionDtoRequest request) {
        AgentEntity agent = find(id);

        agent.updatePosition(request.latitude(), request.longitude(), clock.instant());
        agentRepository.saveAndFlush(agent);

        return new AgentDtoResponse(agent);
    }

    @Transactional(readOnly = true)
    public Page<AgentHistoryDtoResponse> history(UUID id, Pageable pageable) {
        if (!agentRepository.existsById(id)) {
            throw new AgentNotFoundException(id);
        }
        return statusHistoryRepository.findByAgentIdOrderByChangedAtDesc(id, pageable)
                .map(AgentHistoryDtoResponse::new);
    }

    private AgentEntity find(UUID id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new AgentNotFoundException(id));
    }

    /**
     * Regra 2: a viatura existe ({@code 404}), é viatura de agente, é do mesmo órgão e está utilizável
     * ({@code 422}). Agente fora de serviço não fica com viatura (regra 5).
     */
    private VehicleEntity compatibleVehicle(UUID vehicleId, AgentType agentType, AgentStatus status) {
        if (vehicleId == null) {
            return null;
        }
        EquipmentEntity equipment = equipmentRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        if (!(equipment instanceof VehicleEntity vehicle) || !PATROL_VEHICLE_TYPES.contains(vehicle.getType())) {
            throw new BusinessRuleException("O equipamento escolhido não é uma viatura de agente.");
        }
        if (vehicle.getAgency() != agentType.agency()) {
            throw new BusinessRuleException("A viatura pertence a outro órgão.");
        }
        if (UNUSABLE_VEHICLE_STATUSES.contains(vehicle.getStatus())) {
            throw new BusinessRuleException("A viatura está desativada ou roubada.");
        }
        if (!status.isOnDuty()) {
            throw new BusinessRuleException("Agente fora de serviço ou afastado não pode ficar com viatura.");
        }
        return vehicle;
    }

    private static ConflictException duplicatedRegistration(String registrationNumber) {
        return new ConflictException("Já existe um agente com a matrícula " + registrationNumber + ".");
    }
}
