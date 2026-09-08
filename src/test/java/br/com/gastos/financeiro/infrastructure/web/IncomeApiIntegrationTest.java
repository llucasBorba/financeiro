package br.com.gastos.financeiro.infrastructure.web;

import br.com.gastos.financeiro.support.AuthTestClient;
import br.com.gastos.financeiro.support.AuthTestClient.Session;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncomeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String criarReceita(Session session, String valor, String descricao, String data) throws Exception {
        String body = mockMvc.perform(post("/api/incomes")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": %s, "currency": "BRL", "description": "%s", "receivedAt": "%s" }
                                """.formatted(valor, descricao, data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(session.userId().toString()))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("cria, consulta, edita e apaga uma receita")
    void fullIncomeFlow() throws Exception {
        Session session = auth.newUser();
        String id = criarReceita(session, "5000.00", "Salário setembro", "2026-09-05");

        mockMvc.perform(get("/api/incomes/{id}", id).header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Salário setembro"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.currency").value("BRL"));

        mockMvc.perform(put("/api/incomes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 5200.00, "currency": "BRL",
                                  "description": "Salário setembro (com bônus)", "receivedAt": "2026-09-06" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(5200.00))
                .andExpect(jsonPath("$.description").value("Salário setembro (com bônus)"));

        mockMvc.perform(delete("/api/incomes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/incomes/{id}", id).header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("lista do mês trazendo salário e o pix inesperado")
    void listsByPeriod() throws Exception {
        Session session = auth.newUser();
        criarReceita(session, "5000.00", "Salário setembro", "2026-09-05");
        criarReceita(session, "300.00", "Pix do pai", "2026-09-18");
        criarReceita(session, "5000.00", "Salário outubro", "2026-10-05");

        // mais recente primeiro
        mockMvc.perform(get("/api/incomes")
                        .param("startDate", "2026-09-01").param("endDate", "2026-09-30")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Pix do pai"))
                .andExpect(jsonPath("$[1].description").value("Salário setembro"));

        mockMvc.perform(get("/api/incomes").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("descrição é obrigatória")
    void descriptionIsRequired() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(post("/api/incomes")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 100.00, "receivedAt": "2026-09-05" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").isNotEmpty());
    }

    @Test
    @DisplayName("valida valor e data de recebimento")
    void validatesAmountAndDate() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(post("/api/incomes")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": -5, "description": "x" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isNotEmpty())
                .andExpect(jsonPath("$.errors.receivedAt").isNotEmpty());
    }

    @Test
    @DisplayName("moeda em minúscula é normalizada")
    void normalizesCurrency() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(post("/api/incomes")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 100.00, "currency": "brl", "description": "x", "receivedAt": "2026-09-05" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    @DisplayName("para outro usuário, a receita não existe")
    void hidesIncomeFromOtherUser() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String id = criarReceita(dono, "5000.00", "Salário", "2026-09-05");

        mockMvc.perform(get("/api/incomes/{id}", id).header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/incomes/{id}", id).header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/incomes").header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(jsonPath("$.length()").value(0));

        // o dono continua com a receita intacta
        mockMvc.perform(get("/api/incomes/{id}", id).header(HttpHeaders.AUTHORIZATION, dono.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Salário"));
    }

    @Test
    @DisplayName("404 para receita inexistente")
    void notFound() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/api/incomes/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("exige autenticação")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/incomes")).andExpect(status().isUnauthorized());
    }
}
