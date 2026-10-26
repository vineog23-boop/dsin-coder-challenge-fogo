package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class DemoAgentSeederTest {

    private static final String FINGERPRINT = """
            select string_agg(registration_number || ':' || name || ':' || status || ':' || coalesce(vehicle_id::text, '-'),
                              ',' order by registration_number)
            from agent""";

    @Autowired
    private DemoAgentSeeder seeder;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveCriar40AgentesDosTresOrgaosQuandoBancoVazio() {
        List<String> agencies = jdbcTemplate.queryForList("select distinct agency from agent", String.class);
        Integer withVehicle = jdbcTemplate.queryForObject("select count(*) from agent where vehicle_id is not null",
                Integer.class);
        Integer history = jdbcTemplate.queryForObject("select count(*) from agent_status_history", Integer.class);

        assertThat(agentRepository.count()).isEqualTo(40);
        assertThat(agencies).containsExactlyInAnyOrder("MUNICIPAL_TRAFFIC", "MUNICIPAL_GUARD", "MILITARY_POLICE");
        assertThat(withVehicle).isPositive();
        assertThat(history).isGreaterThan(40 * 50);
    }

    @Test
    void deveRespeitarRegrasDeViaturaEmTodosOsVinculos() {
        Integer invalid = jdbcTemplate.queryForObject("""
                select count(*) from agent a
                join equipment e on e.id = a.vehicle_id
                where e.type not in ('TRAFFIC_AGENT_MOTORCYCLE', 'MUNICIPAL_GUARD_VEHICLE', 'MILITARY_POLICE_VEHICLE')
                   or e.agency <> a.agency
                   or e.status in ('DEACTIVATED', 'STOLEN')
                   or a.status not in ('AVAILABLE', 'ON_OCCURRENCE')""", Integer.class);

        assertThat(invalid).isZero();
    }

    @Test
    void naoDeveDuplicarEDeveGerarOsMesmosDadosQuandoRodaDeNovo() {
        String before = jdbcTemplate.queryForObject(FINGERPRINT, String.class);

        seeder.run(null);
        long countAfterSecondRun = agentRepository.count();
        jdbcTemplate.execute("delete from agent_status_history; delete from agent");
        seeder.run(null);
        String afterReseed = jdbcTemplate.queryForObject(FINGERPRINT, String.class);

        assertThat(countAfterSecondRun).isEqualTo(40);
        assertThat(afterReseed).isEqualTo(before);
    }
}
