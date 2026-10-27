package br.com.dsin.challengefogo.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita o botão "Authorize" do Swagger: cola-se o token de {@code POST /api/auth/login} e as rotas
 * protegidas passam a ser chamadas com {@code Authorization: Bearer}.
 * O servidor é relativo ("/"): atrás do nginx o Swagger chama a API pelo mesmo endereço da página,
 * em vez do host interno do Docker ({@code backend:8080}), que o navegador não alcança.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "F.O.G.O. API", version = "1.0",
                description = "Inventário de equipamentos IoT e agentes de Presidente Prudente."),
        servers = @Server(url = "/", description = "Mesmo endereço da página"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
