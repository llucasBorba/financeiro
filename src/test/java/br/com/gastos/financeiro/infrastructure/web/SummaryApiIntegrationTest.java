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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SummaryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String categoriaChamada(Session session, String nome) throws Exception {
        String body = mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andReturn().getResponse().getContentAsString();
        // Um filtro do JsonPath sempre devolve array, mesmo com um único casamento.
        java.util.List<String> ids = JsonPath.read(body, "$[?(@.name == '" + nome + "')].id");
        return ids.get(0);
    }

    private void receita(Session s, String valor, String data) throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": %s, "description": "entrada", "receivedAt": "%s" }
                                """.formatted(valor, data)))
                .andExpect(status().isCreated());
    }

    private void despesaPaga(Session s, String cat, String valor, String vence, String pagoEm) throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoryId": "%s", "amount": %s, "description": "x",
                                  "dueDate": "%s", "paidAt": "%s" }
                                """.formatted(cat, valor, vence, pagoEm)))
                .andExpect(status().isCreated());
    }

    private void despesaPendente(Session s, String cat, String valor, String vence) throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoryId": "%s", "amount": %s, "description": "x", "dueDate": "%s" }
                                """.formatted(cat, valor, vence)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("fecha setembro com entradas, saídas, a pagar e quebra por categoria")
    void closesSeptember() throws Exception {
        Session session = auth.newUser();
        String moradia = categoriaChamada(session, "Moradia");
        String alimentacao = categoriaChamada(session, "Alimentação");

        receita(session, "5000.00", "2026-09-05");
        receita(session, "300.00", "2026-09-18");
        despesaPaga(session, moradia, "1500.00", "2026-09-10", "2026-09-10T09:00:00");
        despesaPaga(session, alimentacao, "900.00", "2026-09-15", "2026-09-15T09:00:00");
        despesaPendente(session, alimentacao, "800.00", "2026-09-25");

        mockMvc.perform(get("/api/summary/monthly").param("month", "2026-09")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].month").value("2026-09"))
                .andExpect(jsonPath("$[0].currency").value("BRL"))
                .andExpect(jsonPath("$[0].received").value(5300.00))
                .andExpect(jsonPath("$[0].paid").value(2400.00))
                .andExpect(jsonPath("$[0].pending").value(800.00))
                .andExpect(jsonPath("$[0].balance").value(2900.00))
                .andExpect(jsonPath("$[0].projectedBalance").value(2100.00))
                .andExpect(jsonPath("$[0].byCategory.length()").value(2))
                .andExpect(jsonPath("$[0].byCategory[0].categoryName").value("Moradia"))
                .andExpect(jsonPath("$[0].byCategory[0].total").value(1500.00))
                .andExpect(jsonPath("$[0].byCategory[0].percentage").value(62.5));
    }

    @Test
    @DisplayName("a conta paga com atraso migra de mês")
    void latePaymentMovesToThePaymentMonth() throws Exception {
        Session session = auth.newUser();
        String moradia = categoriaChamada(session, "Moradia");

        // vence em setembro, paga em outubro
        despesaPaga(session, moradia, "1000.00", "2026-09-10", "2026-10-03T09:00:00");

        // setembro: não é saída (ela já foi paga, então também não é "a pagar")
        mockMvc.perform(get("/api/summary/monthly").param("month", "2026-09")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(jsonPath("$[0].paid").value(0))
                .andExpect(jsonPath("$[0].pending").value(0));

        // outubro: é saída, mesmo tendo vencido em setembro
        mockMvc.perform(get("/api/summary/monthly").param("month", "2026-10")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(jsonPath("$[0].paid").value(1000.00));
    }

    @Test
    @DisplayName("mês sem lançamento devolve fechamento zerado")
    void emptyMonth() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/api/summary/monthly").param("month", "2020-01")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].received").value(0))
                .andExpect(jsonPath("$[0].balance").value(0))
                .andExpect(jsonPath("$[0].byCategory.length()").value(0));
    }

    @Test
    @DisplayName("saldo negativo aparece como negativo")
    void negativeBalance() throws Exception {
        Session session = auth.newUser();
        String moradia = categoriaChamada(session, "Moradia");

        receita(session, "1000.00", "2026-09-05");
        despesaPaga(session, moradia, "1500.00", "2026-09-10", "2026-09-10T09:00:00");

        mockMvc.perform(get("/api/summary/monthly").param("month", "2026-09")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(jsonPath("$[0].balance").value(-500.00));
    }

    @Test
    @DisplayName("resumo de mês passado continua disponível a qualquer momento")
    void pastMonthsRemainAvailable() throws Exception {
        Session session = auth.newUser();
        String moradia = categoriaChamada(session, "Moradia");
        despesaPaga(session, moradia, "700.00", "2024-03-10", "2024-03-10T09:00:00");

        mockMvc.perform(get("/api/summary/monthly").param("month", "2024-03")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paid").value(700.00));
    }

    @Test
    @DisplayName("sem o parâmetro, usa o mês corrente")
    void defaultsToCurrentMonth() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/api/summary/monthly")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(java.time.YearMonth.now().toString()));
    }

    @Test
    @DisplayName("exige autenticação")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/summary/monthly")).andExpect(status().isUnauthorized());
    }
}
