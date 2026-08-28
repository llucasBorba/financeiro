package br.com.gastos.financeiro.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a API de metas de ponta a ponta, incluindo a persistência do saldo acumulado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinancialGoalApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String criarMeta(UUID userId) throws Exception {
        String payload = """
                {
                  "userId": "%s",
                  "title": "Reserva de emergência",
                  "targetAmount": 10000.00,
                  "currency": "BRL",
                  "targetDate": "2027-01-01"
                }
                """.formatted(userId);

        String body = mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentAmount").value(0))
                .andExpect(jsonPath("$.achieved").value(false))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("acumula aportes e marca a meta como atingida")
    void depositsAccumulateAndPersist() throws Exception {
        UUID userId = UUID.randomUUID();
        String goalId = criarMeta(userId);

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"amount\":4000.00,\"currency\":\"BRL\"}".formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(4000.00))
                .andExpect(jsonPath("$.achieved").value(false));

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"amount\":6000.00,\"currency\":\"BRL\"}".formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(10000.00))
                .andExpect(jsonPath("$.achieved").value(true));

        // Relê do banco: o saldo acumulado sobrevive ao ciclo de persistência
        mockMvc.perform(get("/api/goals/{id}", goalId).param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(10000.00))
                .andExpect(jsonPath("$.achieved").value(true));
    }

    @Test
    @DisplayName("recusa aporte em moeda diferente da meta")
    void rejectsCurrencyMismatch() throws Exception {
        UUID userId = UUID.randomUUID();
        String goalId = criarMeta(userId);

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"amount\":100.00,\"currency\":\"USD\"}".formatted(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("lista apenas as metas do próprio usuário")
    void listsOnlyOwnGoals() throws Exception {
        UUID userId = UUID.randomUUID();
        criarMeta(userId);

        mockMvc.perform(get("/api/goals").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/goals").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
