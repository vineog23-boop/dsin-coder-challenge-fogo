package br.com.dsin.challengefogo.equipment.application;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentFilter;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.RadarEntity;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EquipmentSpecificationsTest {

    private static final Pageable TODOS = PageRequest.of(0, 50);

    @Autowired
    private EquipmentRepository repository;

    @BeforeEach
    void prepararDados() {
        repository.deleteAll();
        repository.save(radar("RAD-N-MAL", "Kapsch", "Alfa", Region.NORTH, "Santana", EquipmentStatus.MALFUNCTION));
        repository.save(radar("RAD-N-OK", "Kapsch", "Alfa", Region.NORTH, "Santana", EquipmentStatus.OPERATIONAL));
        repository.save(radar("RAD-S-MAL", "Kapsch", "Alfa", Region.SOUTH, "Moema", EquipmentStatus.MALFUNCTION));
        repository.save(new VehicleEntity(EquipmentType.TAXI, "TAX-001", "Toyota", "Corolla",
                Agency.PRIVATE_OPERATOR, EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null,
                local(Region.CENTER, "Sé"), "ABC1D23", 4, false));
        repository.save(new EquipmentEntity(EquipmentType.AUTOMATED_GATE, "CAN-001", "Gate", "G1",
                Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null,
                local(Region.EAST, "Tatuapé")));
        repository.flush();
    }

    private static Location local(Region region, String bairro) {
        return new Location("01310100", "Rua A", null, bairro, "São Paulo", "SP", region,
                new BigDecimal("-23.561414"), new BigDecimal("-46.655881"));
    }

    private static RadarEntity radar(String serie, String marca, String modelo, Region region, String bairro,
                                     EquipmentStatus status) {
        return new RadarEntity(serie, marca, modelo, Agency.MUNICIPAL_TRAFFIC, status, MaintenanceNeed.NONE, null,
                null, local(region, bairro), RadarModality.RED_LIGHT, null, 2);
    }

    private static EquipmentFilter busca(String search) {
        return new EquipmentFilter(null, null, null, null, null, null, search);
    }

    private List<String> series(EquipmentFilter filter) {
        return repository.findAll(EquipmentSpecifications.from(filter), TODOS).stream()
                .map(EquipmentEntity::getSerialNumber).toList();
    }

    @Test
    void deveFiltrarPorTipoRegiaoESituacaoQuandoCombinados() {
        EquipmentFilter filter = new EquipmentFilter(EquipmentType.RADAR, EquipmentStatus.MALFUNCTION, Region.NORTH,
                null, null, null, null);

        List<String> resultado = series(filter);

        assertThat(resultado).containsExactly("RAD-N-MAL");
    }

    @Test
    void deveAcharPorNumeroDeSerieParcialSemDiferenciarMaiusculas() {
        assertThat(series(busca("n-ok"))).containsExactly("RAD-N-OK");
    }

    @Test
    void deveAcharPorMarcaEModeloQuandoBuscaParcial() {
        assertThat(series(busca("toyo"))).containsExactly("TAX-001");
        assertThat(series(busca("COROL"))).containsExactly("TAX-001");
    }

    @Test
    void deveAcharPorPlacaSemExcluirNaoVeiculosQuandoBuscaPorSerie() {
        assertThat(series(busca("abc1d"))).containsExactly("TAX-001");
        assertThat(series(busca("CAN-0"))).containsExactly("CAN-001");
    }

    @Test
    void naoDeveTratarPorcentoComoCoringaQuandoBusca() {
        assertThat(series(busca("%"))).isEmpty();
        assertThat(series(busca("RAD_N"))).isEmpty();
    }

    @Test
    void deveIgnorarMaiusculasQuandoFiltraPorBairro() {
        EquipmentFilter filter = new EquipmentFilter(null, null, null, "sAnTaNa", null, null, null);

        assertThat(series(filter)).containsExactlyInAnyOrder("RAD-N-MAL", "RAD-N-OK");
    }

    @Test
    void devePaginarQuandoTamanhoMenorQueTotal() {
        Page<EquipmentEntity> pagina = repository.findAll(EquipmentSpecifications.from(busca(null)),
                PageRequest.of(0, 2));

        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getTotalElements()).isEqualTo(5);
    }

    @Test
    void deveDevolverTodosQuandoFiltroVazio() {
        EquipmentFilter vazio = new EquipmentFilter(null, null, null, null, null, null, null);

        assertThat(series(vazio)).hasSize(5);
        assertThat(series(busca("   "))).hasSize(5);
    }
}
