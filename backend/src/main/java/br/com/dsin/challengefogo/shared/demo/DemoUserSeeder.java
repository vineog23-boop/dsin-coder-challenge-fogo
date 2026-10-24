package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.auth.domain.Role;
import br.com.dsin.challengefogo.auth.persistence.UserEntity;
import br.com.dsin.challengefogo.auth.persistence.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria os usuários de demonstração para a comissão entrar sem cadastro. As senhas são públicas
 * (estão no README), por isso o seeder só roda no perfil {@code demo}.
 */
@Component
@Profile("demo")
public class DemoUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("gestor", "gestor123", Role.MANAGER);
        createIfMissing("operador", "operador123", Role.OPERATOR);
    }

    private void createIfMissing(String username, String password, Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new UserEntity(username, passwordEncoder.encode(password), role));
        }
    }
}
