package br.com.dsin.challengefogo.auth.application;

import br.com.dsin.challengefogo.auth.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-bytes!!";
    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

    private final TokenService service = new TokenService(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void deveEmitirTokenComUsuarioPerfilEValidadeDeOitoHorasQuandoDadosValidos() {
        IssuedToken issued = service.issue("gestor", Role.MANAGER);

        Jwt jwt = service.decode(issued.accessToken());

        assertThat(issued.expiresIn()).isEqualTo(Duration.ofHours(8).toSeconds());
        assertThat(jwt.getSubject()).isEqualTo("gestor");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("MANAGER");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
    }

    @Test
    void naoDeveAceitarTokenQuandoExpirado() {
        TokenService nineHoursAgo = new TokenService(SECRET,
                Clock.fixed(NOW.minus(Duration.ofHours(9)), ZoneOffset.UTC));
        String token = nineHoursAgo.issue("operador", Role.OPERATOR).accessToken();

        assertThatThrownBy(() -> service.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void naoDeveAceitarTokenQuandoAssinadoComOutroSegredo() {
        TokenService other = new TokenService("outro-segredo-tambem-com-32-bytes-ou-mais",
                Clock.fixed(NOW, ZoneOffset.UTC));
        String token = other.issue("gestor", Role.MANAGER).accessToken();

        assertThatThrownBy(() -> service.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void naoDeveIniciarQuandoSegredoTemMenosDe32Bytes() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        assertThatThrownBy(() -> new TokenService("curto", clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
