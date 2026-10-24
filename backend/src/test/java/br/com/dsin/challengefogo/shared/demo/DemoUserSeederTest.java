package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.auth.domain.Role;
import br.com.dsin.challengefogo.auth.persistence.UserEntity;
import br.com.dsin.challengefogo.auth.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class DemoUserSeederTest {

    @Autowired
    private DemoUserSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deveCriarGestorEOperadorQuandoAplicacaoSobeNoPerfilDemo() {
        UserEntity gestor = userRepository.findByUsername("gestor").orElseThrow();
        UserEntity operador = userRepository.findByUsername("operador").orElseThrow();

        assertThat(gestor.getRole()).isEqualTo(Role.MANAGER);
        assertThat(passwordEncoder.matches("gestor123", gestor.getPasswordHash())).isTrue();
        assertThat(operador.getRole()).isEqualTo(Role.OPERATOR);
        assertThat(passwordEncoder.matches("operador123", operador.getPasswordHash())).isTrue();
    }

    @Test
    void naoDeveDuplicarUsuariosQuandoSeederRodaNovamente() {
        seeder.run(null);

        assertThat(userRepository.count()).isEqualTo(2);
    }
}
