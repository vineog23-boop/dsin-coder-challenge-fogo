package br.com.dsin.challengefogo.auth.application;

import br.com.dsin.challengefogo.auth.domain.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite e valida o JWT da API (HS256, claim {@code role}). Emissão e validação ficam juntas
 * para garantirem o mesmo segredo e algoritmo; o {@link Clock} injetado permite testar a expiração.
 */
@Service
public class TokenService {

    public static final String ROLE_CLAIM = "role";

    private static final Duration VALIDITY = Duration.ofHours(8);
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final Clock clock;

    public TokenService(@Value("${app.jwt.secret}") String secret, Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 com chave menor que 256 bits é recusado pela Nimbus; falhar na subida deixa a causa clara.
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos " + MIN_SECRET_BYTES + " bytes");
        }
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");

        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(clock);
        this.decoder.setJwtValidator(timestampValidator);
        this.clock = clock;
    }

    public IssuedToken issue(String username, Role role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(VALIDITY))
                .claim(ROLE_CLAIM, role.name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, VALIDITY.toSeconds());
    }

    /** Lança {@link JwtException} se o token estiver expirado, malformado ou com assinatura inválida. */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }
}
