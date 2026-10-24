package br.com.dsin.challengefogo.auth.web;

import br.com.dsin.challengefogo.auth.application.AuthService;
import br.com.dsin.challengefogo.auth.application.InvalidCredentialsException;
import br.com.dsin.challengefogo.auth.application.TokenService;
import br.com.dsin.challengefogo.auth.application.dto.LoginDtoRequest;
import br.com.dsin.challengefogo.auth.application.dto.LoginDtoResponse;
import br.com.dsin.challengefogo.auth.domain.Role;
import br.com.dsin.challengefogo.shared.config.ClockConfig;
import br.com.dsin.challengefogo.shared.security.ProblemAuthenticationEntryPoint;
import br.com.dsin.challengefogo.shared.security.SecurityConfig;
import br.com.dsin.challengefogo.shared.web.ProblemTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class, TokenService.class, ClockConfig.class})
class AuthControllerTest {

    private static final String LOGIN = "/api/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void deveResponder200ComTokenQuandoCredenciaisValidasSemToken() throws Exception {
        when(authService.login(any(LoginDtoRequest.class)))
                .thenReturn(new LoginDtoResponse("jwt-abc", 3600L, Role.MANAGER));

        var result = mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"gestor\",\"password\":\"gestor123\"}"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-abc"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void deveResponder401EmProblemDetailsQuandoCredenciaisInvalidas() throws Exception {
        when(authService.login(any(LoginDtoRequest.class))).thenThrow(new InvalidCredentialsException());

        var result = mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"gestor\",\"password\":\"errada\"}"));

        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.UNAUTHORIZED.toString()))
                .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos."));
    }

    @Test
    void deveResponder400DeValidacaoQuandoUsernameEmBranco() throws Exception {
        var result = mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"   \",\"password\":\"gestor123\"}"));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION.toString()));
        verifyNoInteractions(authService);
    }
}
