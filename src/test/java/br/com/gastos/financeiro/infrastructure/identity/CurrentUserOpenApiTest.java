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
}
