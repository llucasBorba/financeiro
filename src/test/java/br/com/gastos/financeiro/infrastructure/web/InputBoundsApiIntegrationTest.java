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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que valor e data absurdos são recusados na ENTRADA, com 400, e não no INSERT.
 *
 * <p>Estes testes existem por causa de dois bugs de comportamentos bem diferentes:
 *
 * <p><b>Valor.</b> {@code @DecimalMin("0.01")} protegia o piso e nada protegia o teto. Um valor
 * com mais de 13 dígitos inteiros só era recusado pela coluna {@code numeric(15,2)}, o que virava
 * 500 no fim da requisição.
 *
 * <p><b>Data.</b> Bem pior, e invisível para os testes até agora. O Jackson aceita
 * {@code "+999999999-12-31"} (a ISO-8601 permite anos longos com sinal explícito) e o driver
 * pgjdbc traduz {@code LocalDate.MAX} para o {@code infinity} do Postgres. A linha era gravada
 * com <b>201 Created</b> e depois não aparecia em nenhum filtro de mês — nada é {@code <=
 * infinity}. Uma despesa que existe e que nenhum relatório encontra.
 *
 * <p>E o H2 destes testes aceita a data sem reclamar: o {@code MODE=PostgreSQL} emula a sintaxe
 * do Postgres, não a faixa dos tipos. Por isso a validação mora no domínio, e não numa anotação
 * de coluna: é o único lugar que vale igualmente nos dois bancos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InputBoundsApiIntegrationTest {

    /** Maior valor que cabe em NUMERIC(15,2): 13 dígitos inteiros. */
    private static final String TETO = "9999999999999.99";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String umaCategoria(Session session) throws Exception {
        String body = mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private String despesa(Session session, String amount, String dueDate) throws Exception {
        return """
                {
                  "categoryId": "%s",
                  "amount": %s,
                  "currency": "BRL",
                  "description": "Teste de limite",
                  "dueDate": "%s"
                }
                """.formatted(umaCategoria(session), amount, dueDate);
    }

    // ------------------------------------------------------------------ valor

    @Test
    @DisplayName("valor acima do teto de NUMERIC(15,2) é 400, não 500")
    void valorAcimaDoTeto() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despesa(s, "99999999999999999.99", "2026-05-10")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o maior valor que a coluna comporta continua sendo aceito")
    void valorNoTetoExatoEAceito() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despesa(s, TETO, "2026-05-10")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(9999999999999.99));
    }

    // ------------------------------------------------------------------ data

    @Test
    @DisplayName("dueDate no ano 999999999 é 400 — antes era 201 gravando 'infinity'")
    void vencimentoNoAnoMaximoDoLocalDate() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despesa(s, "10.00", "+999999999-12-31")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("dueDate muito antigo também é 400")
    void vencimentoAntesDaFaixa() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despesa(s, "10.00", "1899-12-31")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("as bordas da faixa aceita continuam válidas")
    void bordasDaFaixaSaoAceitas() throws Exception {
        Session s = auth.newUser();
        for (String data : new String[]{"1900-01-01", "2200-12-31"}) {
            mockMvc.perform(post("/api/expenses")
                            .header(HttpHeaders.AUTHORIZATION, s.bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(despesa(s, "10.00", data)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("a data de pagamento também é limitada — paid_at é timestamp e sofre o mesmo")
    void dataDePagamentoForaDaFaixa() throws Exception {
        Session s = auth.newUser();
        String id = JsonPath.read(mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despesa(s, "10.00", "2026-05-10")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/api/expenses/" + id + "/payment")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDate\": \"+999999999-12-31T10:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("receita: receivedAt fora da faixa é 400")
    void receitaForaDaFaixa() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(post("/api/incomes")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100.00,
                                  "currency": "BRL",
                                  "description": "Salário",
                                  "receivedAt": "+999999999-12-31"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("meta: targetDate fora da faixa é 400")
    void metaForaDaFaixa() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(post("/api/goals")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Viagem",
                                  "targetAmount": 5000.00,
                                  "currency": "BRL",
                                  "targetDate": "+999999999-12-31"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
