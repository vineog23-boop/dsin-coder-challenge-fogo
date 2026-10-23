package br.com.dsin.challengefogo.shared.security;

import br.com.dsin.challengefogo.auth.application.TokenService;
import br.com.dsin.challengefogo.auth.domain.Role;
import br.com.dsin.challengefogo.shared.config.ClockConfig;
import br.com.dsin.challengefogo.shared.web.ProblemTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class, TokenService.class, ClockConfig.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Test
    void deveResponder401EmProblemDetailsQuandoGetSemToken() throws Exception {
        var result = mockMvc.perform(get("/api/test"));

        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.UNAUTHORIZED.toString()));
    }

    @Test
    void deveResponder401QuandoTokenInvalido() throws Exception {
        var result = mockMvc.perform(get("/api/test").header(HttpHeaders.AUTHORIZATION, "Bearer nao.eh.jwt"));

        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ProblemTypes.UNAUTHORIZED.toString()));
    }

    @Test
    void devePermitirGetQuandoOperador() throws Exception {
        var result = mockMvc.perform(get("/api/test").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)));

        result.andExpect(status().isOk());
    }

    @Test
    void deveResponder403EmProblemDetailsQuandoOperadorFazPost() throws Exception {
        var result = mockMvc.perform(post("/api/test").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)));

        result.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.FORBIDDEN.toString()));
    }

    @Test
    void devePermitirPostQuandoGestor() throws Exception {
        var result = mockMvc.perform(post("/api/test").header(HttpHeaders.AUTHORIZATION, bearer(Role.MANAGER)));

        result.andExpect(status().isOk());
    }

    @Test
    void devePermitirAtualizarPosicaoDeAgenteQuandoOperador() throws Exception {
        var result = mockMvc.perform(patch("/api/agents/123/position")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)));

        result.andExpect(status().isOk());
    }

    @Test
    void devePermitirLoginQuandoSemToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login"));

        result.andExpect(status().isOk());
    }

    private String bearer(Role role) {
        return "Bearer " + tokenService.issue("usuario", role).accessToken();
    }

    @RestController
    static class TestController {

        @GetMapping("/api/test")
        void read() {
        }

        @PostMapping("/api/test")
        void write() {
        }

        @PatchMapping("/api/agents/{id}/position")
        void position(@PathVariable String id) {
        }

        @PostMapping("/api/auth/login")
        void login() {
        }
    }
}
