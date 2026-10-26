package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class DemoEquipmentSeederTest {

    private static final String FINGERPRINT = """
            select string_agg(serial_number || ':' || latitude || ':' || longitude || ':' || status, ',' order by serial_number)
            from equipment""";

    @Autowired
    private DemoEquipmentSeeder seeder;

    @Autowired
    private DemoAgentSeeder agentSeeder;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveCriar150EquipamentosDeTodosOsTiposERegioesQuandoBancoVazio() {
        List<String> types = jdbcTemplate.queryForList("select distinct type from equipment", String.class);
        List<String> regions = jdbcTemplate.queryForList("select distinct region from equipment", String.class);

        assertThat(equipmentRepository.count()).isEqualTo(150);
        assertThat(types).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(EquipmentType.values()).map(Enum::name).toList());
        assertThat(regions).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(Region.values()).map(Enum::name).toList());
    }

    @Test
    void deveManterCoordenadasDentroDaAreaUrbanaQuandoGera() {
        Integer outside = jdbcTemplate.queryForObject("""
                select count(*) from equipment
                where abs(latitude - (?)) > ? or abs(longitude - (?)) > ?""", Integer.class,
                PrudenteGeo.CENTER_LAT, PrudenteGeo.HALF_HEIGHT, PrudenteGeo.CENTER_LNG, PrudenteGeo.HALF_WIDTH);

        assertThat(outside).isZero();
    }

    @Test
    void deveConcentrarFalhasNoNorteENoPeriodoNoturnoQuandoGeraHistorico() {
        Map<String, Double> failuresPerEquipment = jdbcTemplate.queryForList("""
                        select e.region, count(h.id)::float / count(distinct e.id) as rate
                        from equipment e left join equipment_status_history h
                          on h.equipment_id = e.id and h.to_status = 'MALFUNCTION'
                        group by e.region""").stream()
                .collect(Collectors.toMap(row -> (String) row.get("region"), row -> (Double) row.get("rate")));
        Double nightShare = jdbcTemplate.queryForObject("""
                select avg(case when extract(hour from changed_at at time zone 'America/Sao_Paulo') between 18 and 23
                           then 1.0 else 0.0 end)
                from equipment_status_history where to_status = 'MALFUNCTION'""", Double.class);
        Integer maintenances = jdbcTemplate.queryForObject("select count(*) from equipment_maintenance", Integer.class);

        assertThat(failuresPerEquipment.get("NORTH")).isGreaterThan(failuresPerEquipment.get("SOUTH"));
        assertThat(failuresPerEquipment.get("NORTH")).isGreaterThan(failuresPerEquipment.get("WEST"));
        assertThat(nightShare).isGreaterThan(0.5);
        assertThat(maintenances).isPositive();
    }

    @Test
    void naoDeveDuplicarEDeveGerarOsMesmosDadosQuandoRodaDeNovo() {
        String before = jdbcTemplate.queryForObject(FINGERPRINT, String.class);

        seeder.run(null);
        long countAfterSecondRun = equipmentRepository.count();
        // Agentes apontam para as viaturas: saem primeiro.
        jdbcTemplate.execute("delete from agent_status_history; delete from agent; "
                + "delete from equipment_maintenance; delete from equipment_status_history; "
                + "delete from radar; delete from camera; delete from truck_scale; delete from vehicle; "
                + "delete from scooter; delete from equipment");
        seeder.run(null);
        String afterReseed = jdbcTemplate.queryForObject(FINGERPRINT, String.class);
        agentSeeder.run(null); // devolve o banco ao estado de demonstração completo para os outros testes

        assertThat(countAfterSecondRun).isEqualTo(150);
        assertThat(afterReseed).isEqualTo(before);
    }
}
