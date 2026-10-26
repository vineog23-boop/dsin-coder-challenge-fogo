package br.com.dsin.challengefogo.agent.application;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.agent.application.dto.AgentFilter;
import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AgentSpecificationsTest {

    private static final Pageable TODOS = PageRequest.of(0, 50);

    @Autowired
    private AgentRepository repository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private VehicleEntity viatura;

    @BeforeEach
    void prepararDados() {
        repository.deleteAll();
        viatura = equipmentRepository.save(new VehicleEntity(EquipmentType.MUNICIPAL_GUARD_VEHICLE, "VTR-SPEC-001",
                "Chevrolet", "S10", Agency.MUNICIPAL_GUARD, EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null,
                null, local(Region.NORTH), "GCM1A23", 5, false));
        repository.save(agente("Ana Souza", "GCM-001", AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE,
                Region.NORTH, viatura));
        repository.save(agente("Bruno Lima", "GCM-002", AgentType.MUNICIPAL_GUARD, AgentStatus.OFF_DUTY,
                Region.SOUTH, null));
        repository.save(agente("Carla 100%", "PM-001", AgentType.MILITARY_POLICE, AgentStatus.AVAILABLE,
                Region.NORTH, null));
        repository.save(agente("Diego Alves", "CET-001", AgentType.TRAFFIC_AGENT, AgentStatus.ON_OCCURRENCE,
                Region.CENTER, null));
        repository.flush();
    }

    private static Location local(Region region) {
        return new Location("01310100", "Rua A", null, "Centro", "São Paulo", "SP", region,
                new BigDecimal("-23.561414"), new BigDecimal("-46.655881"));
    }

    private static AgentEntity agente(String nome, String matricula, AgentType tipo, AgentStatus status,
                                      Region regiao, VehicleEntity vehicle) {
        return new AgentEntity(nome, matricula, tipo, status, local(regiao), vehicle);
    }

    private static AgentFilter busca(String search) {
        return new AgentFilter(null, null, null, null, search);
    }

    private List<String> matriculas(AgentFilter filter) {
        return repository.findAll(AgentSpecifications.from(filter), TODOS).stream()
                .map(AgentEntity::getRegistrationNumber).toList();
    }

    @Test
    void deveFiltrarPorOrgaoESituacaoQuandoCombinados() {
        AgentFilter filter = new AgentFilter(null, AgentStatus.AVAILABLE, Agency.MUNICIPAL_GUARD, null, null);

        List<String> resultado = matriculas(filter);

        assertThat(resultado).containsExactly("GCM-001");
    }

    @Test
    void deveFiltrarPelaRegiaoDaBase() {
        AgentFilter filter = new AgentFilter(null, null, null, Region.NORTH, null);

        assertThat(matriculas(filter)).containsExactlyInAnyOrder("GCM-001", "PM-001");
    }

    @Test
    void deveFiltrarPorTipoDeAgente() {
        AgentFilter filter = new AgentFilter(AgentType.TRAFFIC_AGENT, null, null, null, null);

        assertThat(matriculas(filter)).containsExactly("CET-001");
    }

    @Test
    void deveAcharPorNomeParcialSemDiferenciarMaiusculas() {
        assertThat(matriculas(busca("sOuZ"))).containsExactly("GCM-001");
    }

    @Test
    void deveAcharPorMatriculaParcialSemDiferenciarMaiusculas() {
        assertThat(matriculas(busca("gcm-"))).containsExactlyInAnyOrder("GCM-001", "GCM-002");
    }

    @Test
    void deveTratarPorcentoComoTextoLiteralNaBusca() {
        assertThat(matriculas(busca("%"))).containsExactly("PM-001");
    }

    @Test
    void naoDeveRestringirQuandoBuscaEmBrancoOuFiltroVazio() {
        assertThat(matriculas(busca("   "))).hasSize(4);
        assertThat(matriculas(busca(null))).hasSize(4);
    }

    @Test
    void deveDevolverVazioQuandoNadaCorresponde() {
        assertThat(matriculas(busca("inexistente"))).isEmpty();
    }

    @Test
    void devePaginarOrdenandoPorNome() {
        Pageable pagina = PageRequest.of(1, 3, Sort.by("name"));

        Page<AgentEntity> resultado = repository.findAll(AgentSpecifications.from(busca(null)), pagina);

        assertThat(resultado.getTotalElements()).isEqualTo(4);
        assertThat(resultado.getContent()).extracting(AgentEntity::getName).containsExactly("Diego Alves");
    }

    @Test
    void deveTrazerAViaturaDoAgenteNaListagem() {
        AgentFilter filter = new AgentFilter(null, null, null, null, "GCM-001");

        AgentEntity agente = repository.findAll(AgentSpecifications.from(filter), TODOS).getContent().get(0);

        assertThat(agente.getVehicle().getLicensePlate()).isEqualTo("GCM1A23");
    }

    @Test
    void naoDevePermitirMatriculaDuplicada() {
        AgentEntity duplicado = agente("Outro", "GCM-001", AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE,
                Region.NORTH, null);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void naoDevePermitirAgenteForaDeServicoComViaturaNoBanco() {
        AgentEntity invalido = agente("Eva", "GCM-009", AgentType.MUNICIPAL_GUARD, AgentStatus.OFF_DUTY,
                Region.NORTH, viatura);

        assertThatThrownBy(() -> repository.saveAndFlush(invalido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePermitirAgenteDisponivelComViaturaNoBanco() {
        AgentEntity valido = agente("Eva", "GCM-009", AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE,
                Region.NORTH, viatura);

        AgentEntity salvo = repository.saveAndFlush(valido);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getVehicle()).isEqualTo(viatura);
    }
}
