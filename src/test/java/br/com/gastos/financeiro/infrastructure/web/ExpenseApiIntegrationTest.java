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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a API de despesas de ponta a ponta: controller -> use case -> adapter -> banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String criarDespesa(UUID userId) throws Exception {
        String payload = """
                {
                  "userId": "%s",
                  "amount": 250.75,
                  "currency": "BRL",
                  "description": "Conta de energia",
                  "dueDate": "2026-05-10",
                  "type": "VARIABLE"
                }
                """.formatted(userId);

        String body = mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("cria, consulta e paga uma despesa")
    void fullExpenseFlow() throws Exception {
        UUID userId = UUID.randomUUID();
        String expenseId = criarDespesa(userId);

        mockMvc.perform(get("/api/expenses/{id}", expenseId).param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Conta de energia"));

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\"}".formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        // O valor pago fica persistido entre requisições
        mockMvc.perform(get("/api/expenses").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    @DisplayName("retorna 409 ao pagar duas vezes a mesma despesa")
    void conflictOnDoublePayment() throws Exception {
        UUID userId = UUID.randomUUID();
        String expenseId = criarDespesa(userId);
        String payload = "{\"userId\":\"%s\"}".formatted(userId);

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                .contentType(MediaType.APPLICATION_JSON).content(payload));

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta despesa já foi paga."));
    }

    @Test
    @DisplayName("retorna 404 para despesa inexistente")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/expenses/{id}", UUID.randomUUID())
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("retorna 400 detalhando os campos inválidos")
    void validationErrors() throws Exception {
        String payload = """
                {
                  "amount": -5,
                  "dueDate": "2026-05-10",
                  "type": ""
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").isNotEmpty())
                .andExpect(jsonPath("$.errors.amount").isNotEmpty())
                .andExpect(jsonPath("$.errors.type").isNotEmpty());
    }

    @Test
    @DisplayName("retorna 400 para tipo de despesa desconhecido")
    void invalidExpenseType() throws Exception {
        String payload = """
                {
                  "userId": "%s",
                  "amount": 10.00,
                  "dueDate": "2026-05-10",
                  "type": "MENSAL"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    @DisplayName("não expõe a despesa para outro usuário")
    void blocksOtherUser() throws Exception {
        UUID userId = UUID.randomUUID();
        String expenseId = criarDespesa(userId);

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }
}
