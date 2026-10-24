package br.com.dsin.challengefogo.auth.application;

import br.com.dsin.challengefogo.auth.application.dto.LoginDtoRequest;
import br.com.dsin.challengefogo.auth.application.dto.LoginDtoResponse;
import br.com.dsin.challengefogo.auth.domain.Role;
import br.com.dsin.challengefogo.auth.persistence.UserEntity;
import br.com.dsin.challengefogo.auth.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-bytes!!";

    // Custo mínimo do BCrypt para o teste não ficar lento; a aplicação usa o padrão.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenService tokenService = new TokenService(SECRET,
            Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC));
    private final AuthService service = new AuthService(userRepository, passwordEncoder, tokenService);

    @Test
    void deveDevolverTokenComPerfilQuandoCredenciaisCorretas() {
        when(userRepository.findByUsername("gestor"))
                .thenReturn(Optional.of(new UserEntity("gestor", passwordEncoder.encode("gestor123"), Role.MANAGER)));

        LoginDtoResponse response = service.login(new LoginDtoRequest("gestor", "gestor123"));

        assertThat(response.role()).isEqualTo(Role.MANAGER);
        assertThat(response.expiresIn()).isEqualTo(8 * 60 * 60);
        assertThat(tokenService.decode(response.accessToken()).getSubject()).isEqualTo("gestor");
    }

    @Test
    void naoDeveAutenticarQuandoSenhaErrada() {
        when(userRepository.findByUsername("gestor"))
                .thenReturn(Optional.of(new UserEntity("gestor", passwordEncoder.encode("gestor123"), Role.MANAGER)));

        assertThatThrownBy(() -> service.login(new LoginDtoRequest("gestor", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void naoDeveAutenticarQuandoUsuarioNaoExiste() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginDtoRequest("fantasma", "qualquer")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
