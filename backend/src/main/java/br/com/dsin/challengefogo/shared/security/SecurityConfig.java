package br.com.dsin.challengefogo.shared.security;

import br.com.dsin.challengefogo.auth.application.TokenService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sem sessão, autenticada por JWT Bearer. Regra geral: leitura para os dois perfis e escrita
 * só para o gestor; a exceção é a posição do agente, que a telemetria do operador atualiza.
 */
@Configuration
public class SecurityConfig {

    private static final String MANAGER = "MANAGER";
    private static final String OPERATOR = "OPERATOR";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ProblemAuthenticationEntryPoint problemHandler) throws Exception {
        http
                // Sem cookie de sessão não há CSRF a explorar: o token vai no header Authorization.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(MANAGER, OPERATOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/agents/*/position").hasAnyRole(MANAGER, OPERATOR)
                        .requestMatchers("/api/**").hasRole(MANAGER)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(TokenService tokenService) {
        return tokenService::decode;
    }

    /** Converte a claim {@code role} em {@code ROLE_<perfil>}, o formato que {@code hasRole} espera. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
