package br.com.dsin.challengefogo.agent.application;

import br.com.dsin.challengefogo.agent.application.dto.AgentDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.AgentDtoResponse;
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
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");

    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final AgentStatusHistoryRepository statusHistoryRepository = mock(AgentStatusHistoryRepository.class);
    private final EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
    private final AgentService service = new AgentService(agentRepository, statusHistoryRepository,
            equipmentRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    // ---------- criação e regra 2 (viatura) ----------

    @Test
    void deveCriarAgenteComOrgaoDerivadoDoTipoQuandoDadosValidos() {
        when(agentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AgentDtoResponse response = service.create(request(AgentType.MUNICIPAL_GUARD, null, null));

        assertThat(response.agency()).isEqualTo(Agency.MUNICIPAL_GUARD);
        assertThat(response.status()).isEqualTo(AgentStatus.AVAILABLE);
        assertThat(response.vehicle()).isNull();
    }

    @Test
    void deveVincularViaturaQuandoTipoOrgaoESituacaoCompativeis() {
        VehicleEntity vehicle = vehicle(EquipmentType.MILITARY_POLICE_VEHICLE, Agency.MILITARY_POLICE,
                EquipmentStatus.OPERATIONAL);
        when(agentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AgentDtoResponse response = service.create(request(AgentType.MILITARY_POLICE, null, vehicle.getId()));

        assertThat(response.vehicle().licensePlate()).isEqualTo("PMA1B23");
    }

    @Test
    void deveLancarNaoEncontradoQuandoViaturaNaoExiste() {
        UUID vehicleId = UUID.randomUUID();
        when(equipmentRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(AgentType.TRAFFIC_AGENT, null, vehicleId)))
                .isInstanceOf(VehicleNotFoundException.class);
        verify(agentRepository, never()).save(any());
    }

    @Test
    void naoDeveVincularViaturaDaPmAGuardaMunicipal() {
        VehicleEntity vehicle = vehicle(EquipmentType.MILITARY_POLICE_VEHICLE, Agency.MILITARY_POLICE,
                EquipmentStatus.OPERATIONAL);

        assertThatThrownBy(() -> service.create(request(AgentType.MUNICIPAL_GUARD, null, vehicle.getId())))
                .isInstanceOf(BusinessRuleException.class);
        verify(agentRepository, never()).save(any());
    }

    @Test
    void naoDeveVincularVeiculoQueNaoEViatura() {
        VehicleEntity taxi = vehicle(EquipmentType.TAXI, Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.OPERATIONAL);

        assertThatThrownBy(() -> service.create(request(AgentType.TRAFFIC_AGENT, null, taxi.getId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveVincularEquipamentoQueNaoEVeiculo() {
        UUID radarId = UUID.randomUUID();
        EquipmentEntity gate = new EquipmentEntity(EquipmentType.AUTOMATED_GATE, "CAN-1", "Marca", "G1",
                Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null,
                location().toLocation());
        when(equipmentRepository.findById(radarId)).thenReturn(Optional.of(gate));

        assertThatThrownBy(() -> service.create(request(AgentType.TRAFFIC_AGENT, null, radarId)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveVincularViaturaRoubada() {
        VehicleEntity stolen = vehicle(EquipmentType.TRAFFIC_AGENT_MOTORCYCLE, Agency.MUNICIPAL_TRAFFIC,
                EquipmentStatus.STOLEN);

        assertThatThrownBy(() -> service.create(request(AgentType.TRAFFIC_AGENT, null, stolen.getId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveCriarAgenteForaDeServicoComViatura() {
        VehicleEntity vehicle = vehicle(EquipmentType.TRAFFIC_AGENT_MOTORCYCLE, Agency.MUNICIPAL_TRAFFIC,
                EquipmentStatus.OPERATIONAL);

        assertThatThrownBy(() -> service.create(request(AgentType.TRAFFIC_AGENT, AgentStatus.OFF_DUTY, vehicle.getId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveCriarQuandoMatriculaDuplicada() {
        when(agentRepository.existsByRegistrationNumber("MAT-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(AgentType.TRAFFIC_AGENT, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(agentRepository, never()).save(any());
    }

    // ---------- busca e edição ----------

    @Test
    void deveLancarExcecaoQuandoAgenteNaoExiste() {
        UUID id = UUID.randomUUID();
        when(agentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void deveAtualizarDadosEOrgaoQuandoTipoMuda() {
        UUID id = UUID.randomUUID();
        AgentEntity agent = existingAgent(id, AgentType.TRAFFIC_AGENT, AgentStatus.AVAILABLE, null);
        when(agentRepository.findById(id)).thenReturn(Optional.of(agent));
        AgentDtoRequest request = new AgentDtoRequest("Novo Nome", "MAT-2", AgentType.MUNICIPAL_GUARD, null,
                location(), null);

        AgentDtoResponse response = service.update(id, request);

        assertThat(response.name()).isEqualTo("Novo Nome");
        assertThat(response.registrationNumber()).isEqualTo("MAT-2");
        assertThat(response.agency()).isEqualTo(Agency.MUNICIPAL_GUARD);
        assertThat(response.status()).isEqualTo(AgentStatus.AVAILABLE);
    }

    @Test
    void naoDeveAtualizarQuandoSituacaoDiferenteDaAtual() {
        UUID id = UUID.randomUUID();
        when(agentRepository.findById(id))
                .thenReturn(Optional.of(existingAgent(id, AgentType.TRAFFIC_AGENT, AgentStatus.AVAILABLE, null)));

        assertThatThrownBy(() -> service.update(id, request(AgentType.TRAFFIC_AGENT, AgentStatus.OFF_DUTY, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveAtualizarQuandoMatriculaDeOutroAgente() {
        UUID id = UUID.randomUUID();
        when(agentRepository.findById(id))
                .thenReturn(Optional.of(existingAgent(id, AgentType.TRAFFIC_AGENT, AgentStatus.AVAILABLE, null)));
        when(agentRepository.existsByRegistrationNumberAndIdNot("MAT-1", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request(AgentType.TRAFFIC_AGENT, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void naoDeveAtualizarComViaturaDeOutroOrgao() {
        UUID id = UUID.randomUUID();
        when(agentRepository.findById(id))
                .thenReturn(Optional.of(existingAgent(id, AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE, null)));
        VehicleEntity pm = vehicle(EquipmentType.MILITARY_POLICE_VEHICLE, Agency.MILITARY_POLICE,
                EquipmentStatus.OPERATIONAL);

        assertThatThrownBy(() -> service.update(id, request(AgentType.MUNICIPAL_GUARD, null, pm.getId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ---------- situação (regras 4 e 5) ----------

    @Test
    void deveGravarHistoricoEDesvincularViaturaQuandoSaiDeServico() {
        UUID id = UUID.randomUUID();
        VehicleEntity vehicle = vehicle(EquipmentType.MUNICIPAL_GUARD_VEHICLE, Agency.MUNICIPAL_GUARD,
                EquipmentStatus.OPERATIONAL);
        AgentEntity agent = existingAgent(id, AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE, vehicle);
        when(agentRepository.findById(id)).thenReturn(Optional.of(agent));

        AgentDtoResponse response = service.changeStatus(id,
                new AgentStatusChangeDtoRequest(AgentStatus.OFF_DUTY, "fim do turno"), "gestor");

        assertThat(response.status()).isEqualTo(AgentStatus.OFF_DUTY);
        assertThat(response.vehicle()).isNull();
        ArgumentCaptor<AgentStatusHistoryEntity> history = ArgumentCaptor.forClass(AgentStatusHistoryEntity.class);
        verify(statusHistoryRepository).save(history.capture());
        assertThat(history.getValue().getAgentId()).isEqualTo(id);
        assertThat(history.getValue().getFromStatus()).isEqualTo(AgentStatus.AVAILABLE);
        assertThat(history.getValue().getToStatus()).isEqualTo(AgentStatus.OFF_DUTY);
        assertThat(history.getValue().getChangedBy()).isEqualTo("gestor");
        assertThat(history.getValue().getChangedAt()).isEqualTo(NOW);
        assertThat(history.getValue().getNote()).isEqualTo("fim do turno");
    }

    @Test
    void deveManterViaturaQuandoPassaParaOcorrencia() {
        UUID id = UUID.randomUUID();
        VehicleEntity vehicle = vehicle(EquipmentType.MUNICIPAL_GUARD_VEHICLE, Agency.MUNICIPAL_GUARD,
                EquipmentStatus.OPERATIONAL);
        when(agentRepository.findById(id))
                .thenReturn(Optional.of(existingAgent(id, AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE, vehicle)));

        AgentDtoResponse response = service.changeStatus(id,
                new AgentStatusChangeDtoRequest(AgentStatus.ON_OCCURRENCE, null), "gestor");

        assertThat(response.vehicle()).isNotNull();
    }

    @Test
    void naoDeveMudarSituacaoQuandoNovaIgualAAtual() {
        UUID id = UUID.randomUUID();
        when(agentRepository.findById(id))
                .thenReturn(Optional.of(existingAgent(id, AgentType.TRAFFIC_AGENT, AgentStatus.AVAILABLE, null)));

        assertThatThrownBy(() -> service.changeStatus(id,
                new AgentStatusChangeDtoRequest(AgentStatus.AVAILABLE, null), "gestor"))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusHistoryRepository, never()).save(any());
    }

    // ---------- posição ----------

    @Test
    void deveGuardarPosicaoComHorarioQuandoAtualiza() {
        UUID id = UUID.randomUUID();
        when(agentRepository.findById(id))
                .thenReturn(Optional.of(existingAgent(id, AgentType.TRAFFIC_AGENT, AgentStatus.AVAILABLE, null)));

        AgentDtoResponse response = service.updatePosition(id,
                new PositionDtoRequest(new BigDecimal("-22.120000"), new BigDecimal("-51.390000")));

        assertThat(response.currentLatitude()).isEqualByComparingTo("-22.12");
        assertThat(response.currentLongitude()).isEqualByComparingTo("-51.39");
        assertThat(response.positionUpdatedAt()).isEqualTo(NOW);
    }

    // ---------- apoio ----------

    private static LocationDto location() {
        return new LocationDto("19010000", "Rua A", null, "Centro", "Presidente Prudente", "SP", Region.CENTER,
                new BigDecimal("-22.125600"), new BigDecimal("-51.388900"));
    }

    private static AgentDtoRequest request(AgentType type, AgentStatus status, UUID vehicleId) {
        return new AgentDtoRequest("Ana Souza", "MAT-1", type, status, location(), vehicleId);
    }

    private VehicleEntity vehicle(EquipmentType type, Agency agency, EquipmentStatus status) {
        VehicleEntity vehicle = new VehicleEntity(type, "VEI-1", "Marca", "V1", agency, status, MaintenanceNeed.NONE,
                null, null, location().toLocation(), "PMA1B23", 4, false);
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(vehicle, "id", id);
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    private static AgentEntity existingAgent(UUID id, AgentType type, AgentStatus status, VehicleEntity vehicle) {
        AgentEntity agent = new AgentEntity("Ana Souza", "MAT-1", type, status, location().toLocation(), vehicle);
        ReflectionTestUtils.setField(agent, "id", id);
        return agent;
    }
}
