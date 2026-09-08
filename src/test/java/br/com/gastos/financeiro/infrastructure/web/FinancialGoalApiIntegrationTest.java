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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialGoalApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String criarMeta(Session session) throws Exception {
        String body = mockMvc.perform(post("/api/goals")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Reserva de emergência",
                                  "targetAmount": 10000.00,
                                  "currency": "BRL",
                                  "targetDate": "2027-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentAmount").value(0))
                .andExpect(jsonPath("$.achieved").value(false))
                .andExpect(jsonPath("$.userId").value(session.userId().toString()))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("acumula aportes e marca a meta como atingida")
    void depositsAccumulateAndPersist() throws Exception {
        Session session = auth.newUser();
        String goalId = criarMeta(session);

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":4000.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(4000.00))
                .andExpect(jsonPath("$.achieved").value(false));

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":6000.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(10000.00))
                .andExpect(jsonPath("$.achieved").value(true));

        mockMvc.perform(get("/api/goals/{id}", goalId).header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(10000.00))
                .andExpect(jsonPath("$.achieved").value(true));
    }

    @Test
    @DisplayName("recusa aporte em moeda diferente da meta")
    void rejectsCurrencyMismatch() throws Exception {
        Session session = auth.newUser();
        String goalId = criarMeta(session);

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("lista apenas as metas do próprio usuário")
    void listsOnlyOwnGoals() throws Exception {
        Session dono = auth.newUser();
        Session outro = auth.newUser();
        criarMeta(dono);

        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, dono.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, outro.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("recusa acesso sem token")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT corrige a meta e mantém o saldo acumulado")
    void updateKeepsBalance() throws Exception {
        Session session = auth.newUser();
        String goalId = criarMeta(session);

        mockMvc.perform(post("/api/goals/{id}/deposits", goalId)
                .header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":4000.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/goals/{id}", goalId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Reserva maior", "targetAmount": 12000.00,
                                  "currency": "BRL", "targetDate": "2027-06-01" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Reserva maior"))
                .andExpect(jsonPath("$.targetAmount").value(12000.00))
                .andExpect(jsonPath("$.currentAmount").value(4000.00))   // aporte intacto
                .andExpect(jsonPath("$.achieved").value(false));
    }

    @Test
    @DisplayName("DELETE remove a meta")
    void deleteRemovesGoal() throws Exception {
        Session session = auth.newUser();
        String goalId = criarMeta(session);

        mockMvc.perform(delete("/api/goals/{id}", goalId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/{id}", goalId).header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("outro usuário não edita nem apaga a meta alheia")
    void otherUserCannotUpdateOrDelete() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String goalId = criarMeta(dono);

        mockMvc.perform(put("/api/goals/{id}", goalId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "invadida", "targetAmount": 1.00, "currency": "BRL" }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/goals/{id}", goalId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/goals/{id}", goalId).header(HttpHeaders.AUTHORIZATION, dono.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Reserva de emergência"));
    }
}
