package br.com.dsin.challengefogo.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita o botão "Authorize" do Swagger: cola-se o token de {@code POST /api/auth/login} e as rotas
 * protegidas passam a ser chamadas com {@code Authorization: Bearer}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "F.O.G.O. API", version = "1.0",
                description = "Inventário de equipamentos IoT e agentes de Presidente Prudente."),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
