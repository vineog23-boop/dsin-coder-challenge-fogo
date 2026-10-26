package br.com.dsin.challengefogo.equipment.application;

import br.com.dsin.challengefogo.equipment.application.dto.CameraDto;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.HistoryEventDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.RadarDto;
import br.com.dsin.challengefogo.equipment.application.dto.ScooterDto;
import br.com.dsin.challengefogo.equipment.application.dto.StatusChangeDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.VehicleDto;
import br.com.dsin.challengefogo.equipment.domain.CameraResolution;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.domain.ScooterOwnership;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMaintenanceEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMaintenanceRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.persistence.RadarEntity;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
import br.com.dsin.challengefogo.shared.domain.FieldValidationException;
import br.com.dsin.challengefogo.shared.domain.FieldValidationException.FieldViolation;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");

    private final EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
    private final EquipmentStatusHistoryRepository statusHistoryRepository = mock(EquipmentStatusHistoryRepository.class);
    private final EquipmentMaintenanceRepository maintenanceRepository = mock(EquipmentMaintenanceRepository.class);
    private final EquipmentService service = new EquipmentService(equipmentRepository, statusHistoryRepository,
            maintenanceRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    // ---------- criação (regras 1, 2 e 6) ----------

    @Test
    void deveCriarRadarComCaracteristicasQuandoBlocoCombinaComTipo() {
        when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EquipmentDtoRequest request = request(EquipmentType.RADAR, radarSpeed(60), null, null, null);

        EquipmentDtoResponse response = service.create(request);

        ArgumentCaptor<EquipmentEntity> saved = ArgumentCaptor.forClass(EquipmentEntity.class);
        verify(equipmentRepository).save(saved.capture());
        assertThat(saved.getValue()).isExactlyInstanceOf(RadarEntity.class);
        assertThat(response.radar().speedLimitKmh()).isEqualTo(60);
        assertThat(response.status()).isEqualTo(EquipmentStatus.NOT_INSTALLED);
        assertThat(response.maintenanceNeed()).isEqualTo(MaintenanceNeed.NONE);
    }

    @Test
    void deveCriarCancelaSoComDadosComunsQuandoTipoNaoTemBloco() {
        when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EquipmentDtoRequest request = request(EquipmentType.AUTOMATED_GATE, null, null, null, null);

        service.create(request);

        ArgumentCaptor<EquipmentEntity> saved = ArgumentCaptor.forClass(EquipmentEntity.class);
        verify(equipmentRepository).save(saved.capture());
        assertThat(saved.getValue()).isExactlyInstanceOf(EquipmentEntity.class);
        assertThat(saved.getValue().getType()).isEqualTo(EquipmentType.AUTOMATED_GATE);
    }

    @Test
    void naoDeveCriarRadarQuandoVemBlocoDeCamera() {
        CameraDto camera = new CameraDto(true, CameraResolution.FULL_HD_1080, true, false);
        EquipmentDtoRequest request = request(EquipmentType.RADAR, radarSpeed(60), camera, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> assertThat(fields(ex)).containsExactly("camera"));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarRadarQuandoFaltaBlocoRadar() {
        EquipmentDtoRequest request = request(EquipmentType.RADAR, null, null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> assertThat(fields(ex)).containsExactly("radar"));
    }

    @Test
    void naoDeveCriarRadarDeVelocidadeQuandoFaltaLimite() {
        EquipmentDtoRequest request = request(EquipmentType.RADAR, radarSpeed(null), null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> assertThat(fields(ex)).containsExactly("radar.speedLimitKmh"));
    }

    @Test
    void naoDeveCriarCancelaQuandoVemQualquerBloco() {
        EquipmentDtoRequest request = request(EquipmentType.AUTOMATED_GATE, null, null, vehicle("ABC1D23"), null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> assertThat(fields(ex)).containsExactly("vehicle"));
    }

    @Test
    void naoDeveCriarPatineteCompartilhadoQuandoFaltaOperadora() {
        ScooterDto scooter = new ScooterDto(ScooterOwnership.SHARED, null, 80);
        EquipmentDtoRequest request = request(EquipmentType.ELECTRIC_SCOOTER, null, null, null, scooter);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> assertThat(fields(ex)).containsExactly("scooter.operatorName"));
    }

    @Test
    void naoDeveCriarQuandoManutencaoAgendadaSemData() {
        EquipmentDtoRequest request = request(EquipmentType.AUTOMATED_GATE, MaintenanceNeed.SCHEDULED, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(ex -> assertThat(fields(ex)).containsExactly("nextMaintenanceDate"));
    }

    @Test
    void deveCriarQuandoManutencaoAgendadaComDataNoPassado() {
        when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EquipmentDtoRequest request = request(EquipmentType.AUTOMATED_GATE, MaintenanceNeed.SCHEDULED,
                LocalDate.of(2026, 1, 10));

        EquipmentDtoResponse response = service.create(request);

        assertThat(response.nextMaintenanceDate()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void naoDeveCriarQuandoNumeroDeSerieDuplicado() {
        when(equipmentRepository.existsBySerialNumber("SN-1")).thenReturn(true);
        EquipmentDtoRequest request = request(EquipmentType.AUTOMATED_GATE, null, null, null, null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarQuandoPlacaDuplicada() {
        when(equipmentRepository.existsByLicensePlate("ABC1D23")).thenReturn(true);
        EquipmentDtoRequest request = request(EquipmentType.TAXI, null, null, vehicle("ABC1D23"), null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);
        verify(equipmentRepository, never()).save(any());
    }

    // ---------- busca ----------

    @Test
    void deveLancarExcecaoQuandoEquipamentoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(EquipmentNotFoundException.class);
    }

    // ---------- atualização ----------

    @Test
    void deveAtualizarDadosComunsECaracteristicasQuandoTipoIgual() {
        UUID id = UUID.randomUUID();
        RadarEntity radar = existingRadar(id, EquipmentStatus.OPERATIONAL);
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(radar));
        EquipmentDtoRequest request = new EquipmentDtoRequest(EquipmentType.RADAR, "SN-NOVO", "Nova", "R2",
                Agency.MUNICIPAL_TRAFFIC, null, MaintenanceNeed.NONE, null, 30, location(),
                new RadarDto(RadarModality.RED_LIGHT, null, 3), null, null, null, null);

        EquipmentDtoResponse response = service.update(id, request);

        assertThat(response.serialNumber()).isEqualTo("SN-NOVO");
        assertThat(response.brand()).isEqualTo("Nova");
        assertThat(response.maintenanceIntervalDays()).isEqualTo(30);
        assertThat(response.radar().modality()).isEqualTo(RadarModality.RED_LIGHT);
        assertThat(response.radar().lanes()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(EquipmentStatus.OPERATIONAL);
    }

    @Test
    void naoDeveAtualizarQuandoTipoDiferenteDoAtual() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(existingRadar(id, EquipmentStatus.OPERATIONAL)));
        EquipmentDtoRequest request = request(EquipmentType.AUTOMATED_GATE, null, null, null, null);

        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveAtualizarQuandoSituacaoDiferenteDaAtual() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(existingRadar(id, EquipmentStatus.OPERATIONAL)));
        EquipmentDtoRequest request = new EquipmentDtoRequest(EquipmentType.RADAR, "SN-1", "Marca", "R1",
                Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.MALFUNCTION, null, null, null, location(),
                radarSpeed(60), null, null, null, null);

        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveAtualizarQuandoNumeroDeSerieDeOutroEquipamento() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(existingRadar(id, EquipmentStatus.OPERATIONAL)));
        when(equipmentRepository.existsBySerialNumberAndIdNot("SN-1", id)).thenReturn(true);
        EquipmentDtoRequest request = request(EquipmentType.RADAR, radarSpeed(60), null, null, null);

        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void naoDeveAtualizarQuandoPlacaDeOutroVeiculo() {
        UUID id = UUID.randomUUID();
        VehicleEntity taxi = new VehicleEntity(EquipmentType.TAXI, "SN-1", "Marca", "T1", Agency.PRIVATE_OPERATOR,
                EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null, location().toLocation(),
                "AAA1A11", 4, false);
        ReflectionTestUtils.setField(taxi, "id", id);
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(taxi));
        when(equipmentRepository.existsByLicensePlateAndIdNot("ABC1D23", id)).thenReturn(true);
        EquipmentDtoRequest request = request(EquipmentType.TAXI, null, null, vehicle("ABC1D23"), null);

        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(ConflictException.class);
    }

    // ---------- situação (regras 3 e 4) ----------

    @Test
    void deveExigirManutencaoImediataEGravarHistoricoQuandoMudaParaFalha() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(existingRadar(id, EquipmentStatus.OPERATIONAL)));

        EquipmentDtoResponse response = service.changeStatus(id,
                new StatusChangeDtoRequest(EquipmentStatus.MALFUNCTION, "Sem imagem"), "gestor");

        assertThat(response.status()).isEqualTo(EquipmentStatus.MALFUNCTION);
        assertThat(response.maintenanceNeed()).isEqualTo(MaintenanceNeed.IMMEDIATE);
        ArgumentCaptor<EquipmentStatusHistoryEntity> history = ArgumentCaptor.forClass(EquipmentStatusHistoryEntity.class);
        verify(statusHistoryRepository).save(history.capture());
        assertThat(history.getValue().getEquipmentId()).isEqualTo(id);
        assertThat(history.getValue().getFromStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        assertThat(history.getValue().getToStatus()).isEqualTo(EquipmentStatus.MALFUNCTION);
        assertThat(history.getValue().getChangedBy()).isEqualTo("gestor");
        assertThat(history.getValue().getChangedAt()).isEqualTo(NOW);
        assertThat(history.getValue().getNote()).isEqualTo("Sem imagem");
    }

    @Test
    void naoDeveMudarSituacaoQuandoNovaIgualAAtual() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(existingRadar(id, EquipmentStatus.OPERATIONAL)));

        assertThatThrownBy(() -> service.changeStatus(id,
                new StatusChangeDtoRequest(EquipmentStatus.OPERATIONAL, null), "gestor"))
                .isInstanceOf(BusinessRuleException.class);
        verify(statusHistoryRepository, never()).save(any());
    }

    // ---------- manutenção (regra 5) ----------

    @Test
    void deveVoltarAOperarEAgendarProximaQuandoManutencaoEmEquipamentoComFalha() {
        UUID id = UUID.randomUUID();
        RadarEntity radar = existingRadar(id, EquipmentStatus.OPERATIONAL);
        radar.changeStatus(EquipmentStatus.MALFUNCTION);
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(radar));
        when(maintenanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Instant performedAt = Instant.parse("2026-10-25T13:00:00Z");

        service.registerMaintenance(id,
                new MaintenanceDtoRequest(MaintenanceKind.CORRECTIVE, performedAt, "Troca da câmera"), "gestor");

        assertThat(radar.getStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        assertThat(radar.getMaintenanceNeed()).isEqualTo(MaintenanceNeed.SCHEDULED);
        assertThat(radar.getNextMaintenanceDate()).isEqualTo(LocalDate.of(2026, 11, 24));
        ArgumentCaptor<EquipmentStatusHistoryEntity> history = ArgumentCaptor.forClass(EquipmentStatusHistoryEntity.class);
        verify(statusHistoryRepository).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isEqualTo(EquipmentStatus.MALFUNCTION);
        assertThat(history.getValue().getToStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        ArgumentCaptor<EquipmentMaintenanceEntity> maintenance = ArgumentCaptor.forClass(EquipmentMaintenanceEntity.class);
        verify(maintenanceRepository).save(maintenance.capture());
        assertThat(maintenance.getValue().getPerformedBy()).isEqualTo("gestor");
        assertThat(maintenance.getValue().getEquipmentId()).isEqualTo(id);
    }

    @Test
    void deveZerarPendenciaSemMudarSituacaoQuandoManutencaoSemIntervaloEmEquipamentoOperando() {
        UUID id = UUID.randomUUID();
        EquipmentEntity gate = new EquipmentEntity(EquipmentType.AUTOMATED_GATE, "SN-2", "Marca", "G1",
                Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                LocalDate.of(2026, 10, 1), null, location().toLocation());
        ReflectionTestUtils.setField(gate, "id", id);
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(gate));
        when(maintenanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerMaintenance(id,
                new MaintenanceDtoRequest(MaintenanceKind.PREVENTIVE, NOW, "Lubrificação"), "gestor");

        assertThat(gate.getStatus()).isEqualTo(EquipmentStatus.OPERATIONAL);
        assertThat(gate.getMaintenanceNeed()).isEqualTo(MaintenanceNeed.NONE);
        assertThat(gate.getNextMaintenanceDate()).isNull();
        verify(statusHistoryRepository, never()).save(any());
    }

    // ---------- histórico ----------

    @Test
    void deveDevolverSituacoesEManutencoesDoMaisRecenteParaOMaisAntigoQuandoConsultaHistorico() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.existsById(id)).thenReturn(true);
        Pageable firstTwo = PageRequest.of(0, 2);
        when(statusHistoryRepository.findByEquipmentIdOrderByChangedAtDesc(eq(id), any())).thenReturn(page(List.of(
                new EquipmentStatusHistoryEntity(id, EquipmentStatus.MALFUNCTION, EquipmentStatus.OPERATIONAL,
                        Instant.parse("2026-10-20T10:00:00Z"), "gestor", null),
                new EquipmentStatusHistoryEntity(id, EquipmentStatus.OPERATIONAL, EquipmentStatus.MALFUNCTION,
                        Instant.parse("2026-10-01T10:00:00Z"), "gestor", null))));
        when(maintenanceRepository.findByEquipmentIdOrderByPerformedAtDesc(eq(id), any())).thenReturn(page(List.of(
                new EquipmentMaintenanceEntity(id, MaintenanceKind.CORRECTIVE, Instant.parse("2026-10-20T09:00:00Z"),
                        "Reparo", "gestor"))));

        Page<HistoryEventDtoResponse> history = service.history(id, firstTwo);

        assertThat(history.getContent()).extracting(HistoryEventDtoResponse::occurredAt).containsExactly(
                Instant.parse("2026-10-20T10:00:00Z"), Instant.parse("2026-10-20T09:00:00Z"));
        assertThat(history.getTotalElements()).isEqualTo(3);
    }

    @Test
    void deveLancarExcecaoQuandoHistoricoDeEquipamentoInexistente() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.history(id, PageRequest.of(0, 20)))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    // ---------- apoio ----------

    private static <T> Page<T> page(List<T> content) {
        return new PageImpl<>(content, PageRequest.of(0, 10), content.size());
    }

    private static List<String> fields(Throwable ex) {
        return ((FieldValidationException) ex).getViolations().stream().map(FieldViolation::field).toList();
    }

    private static LocationDto location() {
        return new LocationDto("19010000", "Avenida Washington Luiz", "número 181", "Centro", "Presidente Prudente",
                "SP", Region.CENTER, new BigDecimal("-22.125600"), new BigDecimal("-51.388900"));
    }

    private static RadarDto radarSpeed(Integer speedLimitKmh) {
        return new RadarDto(RadarModality.SPEED, speedLimitKmh, 2);
    }

    private static VehicleDto vehicle(String licensePlate) {
        return new VehicleDto(licensePlate, 4, false);
    }

    private static EquipmentDtoRequest request(EquipmentType type, RadarDto radar, CameraDto camera, VehicleDto vehicle,
                                               ScooterDto scooter) {
        return new EquipmentDtoRequest(type, "SN-1", "Marca", "M1", Agency.MUNICIPAL_TRAFFIC, null, null, null, null,
                location(), radar, camera, null, vehicle, scooter);
    }

    private static EquipmentDtoRequest request(EquipmentType type, MaintenanceNeed need, LocalDate nextDate) {
        return new EquipmentDtoRequest(type, "SN-1", "Marca", "M1", Agency.MUNICIPAL_TRAFFIC, null, need, nextDate,
                null, location(), null, null, null, null, null);
    }

    private static RadarEntity existingRadar(UUID id, EquipmentStatus status) {
        RadarEntity radar = new RadarEntity("SN-1", "Marca", "R1", Agency.MUNICIPAL_TRAFFIC, status,
                MaintenanceNeed.NONE, null, 30, location().toLocation(), RadarModality.SPEED, 60, 2);
        ReflectionTestUtils.setField(radar, "id", id);
        return radar;
    }
}
