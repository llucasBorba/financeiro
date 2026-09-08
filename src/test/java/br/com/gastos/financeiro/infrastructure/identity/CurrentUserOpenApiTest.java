package br.com.gastos.financeiro.infrastructure.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que {@link CurrentUser} não vaze para o contrato público da API.
 *
 * <p>O springdoc não conhece anotações de terceiros: sem a configuração em
 * {@link CurrentUserOpenApiConfig}, ele enxerga um {@code UUID} sem anotação conhecida,
 * conclui que é um {@code @RequestParam} implícito e publica um query param {@code userId}
 * obrigatório. A API continua funcionando — mas o Swagger UI passa a exigir o preenchimento
 * de um campo inexistente e nenhuma rota pode ser testada por lá.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CurrentUserOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("nenhuma operação expõe 'userId' como parâmetro")
    void doesNotLeakUserIdParameter() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..parameters[?(@.name == 'userId')]").isEmpty());
    }

    @Test
    @DisplayName("nenhum corpo de requisição pede 'userId'")
    void doesNotLeakUserIdInRequestBodies() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CreateExpenseRequest.properties.userId").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateGoalRequest.properties.userId").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.DepositRequest.properties.userId").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.MarkAsPaidRequest.properties.userId").doesNotExist());
    }

    @Test
    @DisplayName("documenta o esquema Bearer para o Swagger oferecer o botão Authorize")
    void documentsBearerScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    @DisplayName("o header de desenvolvimento X-User-Id não existe mais no contrato")
    void devHeaderIsGone() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..parameters[?(@.name == 'X-User-Id')]").isEmpty());
    }

    @Test
    @DisplayName("toda rota publicada tem um resumo no contrato")
    void everyOperationHasASummary() throws Exception {
        String contrato = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.jayway.jsonpath.DocumentContext doc = com.jayway.jsonpath.JsonPath.parse(contrato);
        java.util.Map<String, java.util.Map<String, Object>> paths = doc.read("$.paths");

        java.util.List<String> semResumo = new java.util.ArrayList<>();
        paths.forEach((rota, verbos) -> verbos.forEach((verbo, operacao) -> {
            Object resumo = ((java.util.Map<?, ?>) operacao).get("summary");
            if (resumo == null || resumo.toString().isBlank()) {
                semResumo.add(verbo.toUpperCase() + " " + rota);
            }
        }));

        // Uma rota sem resumo aparece no Swagger como um verbo e um caminho, e nada mais —
        // quem chega de fora precisa ler o código para saber o que ela faz.
        org.junit.jupiter.api.Assertions.assertTrue(semResumo.isEmpty(),
                "rotas sem summary: " + semResumo);
    }
}
