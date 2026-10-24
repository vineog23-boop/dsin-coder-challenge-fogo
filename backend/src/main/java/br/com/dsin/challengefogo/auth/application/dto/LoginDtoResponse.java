package br.com.dsin.challengefogo.auth.application.dto;

import br.com.dsin.challengefogo.auth.application.IssuedToken;
import br.com.dsin.challengefogo.auth.domain.Role;

/** O perfil volta junto com o token para o front decidir o que mostrar sem decodificar o JWT. */
public record LoginDtoResponse(String accessToken, long expiresIn, Role role) {

    public LoginDtoResponse(IssuedToken token, Role role) {
        this(token.accessToken(), token.expiresIn(), role);
    }
}
