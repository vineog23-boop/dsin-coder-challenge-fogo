package br.com.dsin.challengefogo.shared.web;

import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
import br.com.dsin.challengefogo.shared.domain.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Filtros desligados: a segurança entra no T5 e não é o assunto deste teste.
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveResponder400ComCamposInvalidosQuandoCorpoFalhaNaValidacao() throws Exception {
        String body = """
                { "name": "" }
                """;

        var result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION.toString()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("nome é obrigatório"));
    }

    @Test
    void deveResponder404QuandoRecursoNaoExiste() throws Exception {
        var result = mockMvc.perform(get("/test/not-found"));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND.toString()))
                .andExpect(jsonPath("$.detail").value("Equipamento não encontrado"));
    }

    @Test
    void deveResponder409QuandoHaConflito() throws Exception {
        var result = mockMvc.perform(get("/test/conflict"));

        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemTypes.CONFLICT.toString()))
                .andExpect(jsonPath("$.detail").value("Número de série já cadastrado"));
    }

    @Test
    void deveResponder422QuandoRegraDeNegocioEhViolada() throws Exception {
        var result = mockMvc.perform(get("/test/business-rule"));

        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(ProblemTypes.BUSINESS_RULE.toString()))
                .andExpect(jsonPath("$.detail").value("Equipamento já está nesta situação"));
    }

    @Test
    void naoDeveExporDetalhesInternosQuandoErroInesperado() throws Exception {
        var result = mockMvc.perform(get("/test/unexpected"));

        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value(ProblemTypes.INTERNAL.toString()));
        String body = result.andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("segredo interno", "IllegalStateException", "at br.com", "trace");
    }

    record TestDtoRequest(@NotBlank(message = "nome é obrigatório") String name) {
    }

    @RestController
    static class TestController {

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody TestDtoRequest request) {
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("Equipamento não encontrado");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Número de série já cadastrado");
        }

        @GetMapping("/test/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Equipamento já está nesta situação");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("segredo interno");
        }
    }
}
