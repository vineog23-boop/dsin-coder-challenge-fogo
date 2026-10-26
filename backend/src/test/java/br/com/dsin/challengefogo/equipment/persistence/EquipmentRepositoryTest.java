package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.equipment.domain.CameraResolution;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.domain.ScooterOwnership;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EquipmentRepositoryTest {

    private static final String[] TABELAS_FILHAS = {"radar", "camera", "truck_scale", "vehicle", "scooter"};

    @Autowired
    private EquipmentRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Location localizacao() {
        return new Location("01310100", "Avenida Paulista", "Próximo ao MASP", "Bela Vista", "São Paulo", "SP",
                Region.CENTER, new BigDecimal("-23.561414"), new BigDecimal("-46.655881"));
    }

    private void assertLocalizacao(Location lida) {
        assertThat(lida.getZipCode()).isEqualTo("01310100");
        assertThat(lida.getStreet()).isEqualTo("Avenida Paulista");
        assertThat(lida.getReference()).isEqualTo("Próximo ao MASP");
        assertThat(lida.getNeighborhood()).isEqualTo("Bela Vista");
        assertThat(lida.getCity()).isEqualTo("São Paulo");
        assertThat(lida.getState()).isEqualTo("SP");
        assertThat(lida.getRegion()).isEqualTo(Region.CENTER);
        assertThat(lida.getLatitude()).isEqualTo(new BigDecimal("-23.561414"));
        assertThat(lida.getLatitude().scale()).isEqualTo(6);
        assertThat(lida.getLongitude()).isEqualTo(new BigDecimal("-46.655881"));
        assertThat(lida.getLongitude().scale()).isEqualTo(6);
    }

    private EquipmentEntity salvarERele(EquipmentEntity equipamento) {
        UUID id = repository.save(equipamento).getId();
        entityManager.flush();
        entityManager.clear();

        return repository.findById(id).orElseThrow();
    }

    @Test
    void deveRelerRadarComCaracteristicasELocalizacaoQuandoSalvo() {
        RadarEntity radar = new RadarEntity("RAD-001", "Marca", "R1", Agency.MUNICIPAL_TRAFFIC,
                EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, 90, localizacao(),
                RadarModality.SPEED, 60, 2);

        EquipmentEntity lido = salvarERele(radar);

        assertThat(lido).isExactlyInstanceOf(RadarEntity.class);
        RadarEntity lidoRadar = (RadarEntity) lido;
        assertThat(lidoRadar.getType()).isEqualTo(EquipmentType.RADAR);
        assertThat(lidoRadar.getSerialNumber()).isEqualTo("RAD-001");
        assertThat(lidoRadar.getModality()).isEqualTo(RadarModality.SPEED);
        assertThat(lidoRadar.getSpeedLimitKmh()).isEqualTo(60);
        assertThat(lidoRadar.getLanes()).isEqualTo(2);
        assertThat(lidoRadar.getMaintenanceIntervalDays()).isEqualTo(90);
        assertLocalizacao(lidoRadar.getLocation());
    }

    @Test
    void deveRelerCameraComCaracteristicasELocalizacaoQuandoSalva() {
        CameraEntity camera = new CameraEntity("CAM-001", "Marca", "C1", Agency.MUNICIPAL_GUARD,
                EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null, localizacao(),
                true, CameraResolution.UHD_4K, true, false);

        EquipmentEntity lido = salvarERele(camera);

        assertThat(lido).isExactlyInstanceOf(CameraEntity.class);
        CameraEntity lidaCamera = (CameraEntity) lido;
        assertThat(lidaCamera.getType()).isEqualTo(EquipmentType.SURVEILLANCE_CAMERA);
        assertThat(lidaCamera.isNightVision()).isTrue();
        assertThat(lidaCamera.getResolution()).isEqualTo(CameraResolution.UHD_4K);
        assertThat(lidaCamera.isColor()).isTrue();
        assertThat(lidaCamera.isPtz()).isFalse();
        assertLocalizacao(lidaCamera.getLocation());
    }

    @Test
    void deveRelerBalancaComCaracteristicasELocalizacaoQuandoSalva() {
        TruckScaleEntity balanca = new TruckScaleEntity("BAL-001", "Marca", "B1", Agency.MUNICIPAL_TRAFFIC,
                EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null, localizacao(),
                new BigDecimal("45.50"), true);

        EquipmentEntity lido = salvarERele(balanca);

        assertThat(lido).isExactlyInstanceOf(TruckScaleEntity.class);
        TruckScaleEntity lidaBalanca = (TruckScaleEntity) lido;
        assertThat(lidaBalanca.getType()).isEqualTo(EquipmentType.TRUCK_SCALE);
        assertThat(lidaBalanca.getMaxCapacityTons()).isEqualByComparingTo("45.50");
        assertThat(lidaBalanca.isWeighInMotion()).isTrue();
        assertLocalizacao(lidaBalanca.getLocation());
    }

    @Test
    void deveRelerVeiculoComCaracteristicasELocalizacaoQuandoSalvo() {
        VehicleEntity veiculo = new VehicleEntity(EquipmentType.TAXI, "VEI-001", "Marca", "V1",
                Agency.PRIVATE_OPERATOR, EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                LocalDate.of(2026, 12, 1), 180, localizacao(), "ABC1D23", 4, false);

        EquipmentEntity lido = salvarERele(veiculo);

        assertThat(lido).isExactlyInstanceOf(VehicleEntity.class);
        VehicleEntity lidoVeiculo = (VehicleEntity) lido;
        assertThat(lidoVeiculo.getType()).isEqualTo(EquipmentType.TAXI);
        assertThat(lidoVeiculo.getLicensePlate()).isEqualTo("ABC1D23");
        assertThat(lidoVeiculo.getPassengerCapacity()).isEqualTo(4);
        assertThat(lidoVeiculo.isAutonomous()).isFalse();
        assertThat(lidoVeiculo.getMaintenanceNeed()).isEqualTo(MaintenanceNeed.SCHEDULED);
        assertThat(lidoVeiculo.getNextMaintenanceDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertLocalizacao(lidoVeiculo.getLocation());
    }

    @Test
    void deveRelerPatineteComCaracteristicasELocalizacaoQuandoSalvo() {
        ScooterEntity patinete = new ScooterEntity("PAT-001", "Marca", "P1", Agency.PRIVATE_OPERATOR,
                EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null, localizacao(),
                ScooterOwnership.SHARED, "Operadora X", 80);

        EquipmentEntity lido = salvarERele(patinete);

        assertThat(lido).isExactlyInstanceOf(ScooterEntity.class);
        ScooterEntity lidoPatinete = (ScooterEntity) lido;
        assertThat(lidoPatinete.getType()).isEqualTo(EquipmentType.ELECTRIC_SCOOTER);
        assertThat(lidoPatinete.getOwnership()).isEqualTo(ScooterOwnership.SHARED);
        assertThat(lidoPatinete.getOperatorName()).isEqualTo("Operadora X");
        assertThat(lidoPatinete.getBatteryLevel()).isEqualTo(80);
        assertLocalizacao(lidoPatinete.getLocation());
    }

    @Test
    void deveManterSoNaTabelaBaseQuandoSalvaCancelaAutomatica() {
        EquipmentEntity cancela = new EquipmentEntity(EquipmentType.AUTOMATED_GATE, "CAN-001", "Marca", "G1",
                Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null,
                localizacao());

        EquipmentEntity lido = salvarERele(cancela);

        assertThat(lido).isExactlyInstanceOf(EquipmentEntity.class);
        assertThat(lido.getType()).isEqualTo(EquipmentType.AUTOMATED_GATE);
        assertLocalizacao(lido.getLocation());
        for (String tabela : TABELAS_FILHAS) {
            Integer linhas = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + tabela + " WHERE id = ?", Integer.class, lido.getId());
            assertThat(linhas).as("linhas em %s", tabela).isZero();
        }
    }

    @Test
    void devePreencherCreatedAtEUpdatedAtQuandoSalvo() {
        EquipmentEntity cancela = new EquipmentEntity(EquipmentType.PARKING_KIOSK, "TOT-001", "Marca", "K1",
                Agency.MUNICIPAL_TRAFFIC, EquipmentStatus.NOT_INSTALLED, MaintenanceNeed.NONE, null, null, null);

        EquipmentEntity lido = salvarERele(cancela);

        assertThat(lido.getCreatedAt()).isNotNull();
        assertThat(lido.getUpdatedAt()).isNotNull();
    }
}
