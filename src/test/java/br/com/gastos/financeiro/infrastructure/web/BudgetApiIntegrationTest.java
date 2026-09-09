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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BudgetApiIntegrationTest {

    private static final String MES = "2026-09";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String umaCategoria(Session s) throws Exception {
        String body = mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private String outraCategoria(Session s) throws Exception {
        String body = mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[1].id");
    }

    private void definirLimite(Session s, String categoriaId, String valor) throws Exception {
        mockMvc.perform(put("/api/budgets/{categoryId}", categoriaId)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyLimit\":" + valor + ",\"currency\":\"BRL\"}"))
                .andExpect(status().isOk());
    }

    /** Cria a despesa e, se pedido, marca como paga dentro do mês consultado. */
    private void despesa(Session s, String categoriaId, String valor, String vencimento, boolean paga)
            throws Exception {
        String body = mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","amount":%s,"currency":"BRL",
                                 "description":"teste","dueDate":"%s"}
                                """.formatted(categoriaId, valor, vencimento)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        if (paga) {
            mockMvc.perform(patch("/api/expenses/{id}/payment", JsonPath.read(body, "$.id").toString())
                            .header(HttpHeaders.AUTHORIZATION, s.bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"paymentDate\":\"2026-09-05T10:00:00\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("confronta o limite com o gasto e o que está a pagar")
    void confrontaLimiteComOMes() throws Exception {
        Session s = auth.newUser();
        String categoria = umaCategoria(s);

        definirLimite(s, categoria, "1000.00");
        despesa(s, categoria, "500.00", "2026-09-03", true);    // paga dentro do mês
        despesa(s, categoria, "400.00", "2026-09-20", false);   // em aberto, vence no mês

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].monthlyLimit").value(1000.00))
                .andExpect(jsonPath("$[0].spent").value(500.00))
                .andExpect(jsonPath("$[0].pending").value(400.00))
                .andExpect(jsonPath("$[0].remaining").value(500.00))
                .andExpect(jsonPath("$[0].usedPercentage").value(50.00))
                .andExpect(jsonPath("$[0].projectedPercentage").value(90.00))
                .andExpect(jsonPath("$[0].exceeded").value(false))
                .andExpect(jsonPath("$[0].projectedToExceed").value(false));
    }

    @Test
    @DisplayName("avisa que estourou quando o pago passa do limite")
    void avisaEstouro() throws Exception {
        Session s = auth.newUser();
        String categoria = umaCategoria(s);

        definirLimite(s, categoria, "300.00");
        despesa(s, categoria, "450.00", "2026-09-03", true);

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exceeded").value(true))
                .andExpect(jsonPath("$[0].remaining").value(-150.00));
    }

    @Test
    @DisplayName("estourar o limite NÃO impede cadastrar a despesa")
    void estouroNaoBloqueia() throws Exception {
        Session s = auth.newUser();
        String categoria = umaCategoria(s);

        definirLimite(s, categoria, "10.00");

        // O app existe para refletir o que aconteceu; uma conta que chegou, chegou.
        despesa(s, categoria, "9999.00", "2026-09-15", false);

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$[0].projectedToExceed").value(true));
    }

    @Test
    @DisplayName("definir o limite duas vezes atualiza, não duplica")
    void definirDuasVezesEIdempotente() throws Exception {
        Session s = auth.newUser();
        String categoria = umaCategoria(s);

        definirLimite(s, categoria, "1000.00");
        definirLimite(s, categoria, "2500.00");

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].monthlyLimit").value(2500.00));
    }

    @Test
    @DisplayName("categoria sem limite não aparece na lista")
    void categoriaSemLimiteNaoAparece() throws Exception {
        Session s = auth.newUser();
        definirLimite(s, umaCategoria(s), "1000.00");
        despesa(s, outraCategoria(s), "800.00", "2026-09-10", true);

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("remove o limite sem tocar na categoria")
    void removeOLimite() throws Exception {
        Session s = auth.newUser();
        String categoria = umaCategoria(s);
        definirLimite(s, categoria, "1000.00");

        mockMvc.perform(delete("/api/budgets/{categoryId}", categoria)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$.length()").value(0));

        // A categoria continua lá.
        mockMvc.perform(get("/api/categories/{id}", categoria)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("não aceita limite em categoria de outro dono")
    void recusaCategoriaDeOutroDono() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();

        mockMvc.perform(put("/api/budgets/{categoryId}", umaCategoria(dono))
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyLimit\":100.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("orçamento de um usuário não vaza para outro")
    void naoVazaEntreUsuarios() throws Exception {
        Session a = auth.newUser();
        Session b = auth.newUser();
        definirLimite(a, umaCategoria(a), "1000.00");

        mockMvc.perform(get("/api/budgets").param("month", MES)
                        .header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("recusa limite zero ou negativo")
    void recusaLimiteInvalido() throws Exception {
        Session s = auth.newUser();
        String categoria = umaCategoria(s);

        for (String valor : new String[]{"0", "-50.00"}) {
            mockMvc.perform(put("/api/budgets/{categoryId}", categoria)
                            .header(HttpHeaders.AUTHORIZATION, s.bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"monthlyLimit\":" + valor + ",\"currency\":\"BRL\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("remover limite inexistente é 404")
    void removerInexistenteE404() throws Exception {
        Session s = auth.newUser();
        mockMvc.perform(delete("/api/budgets/{categoryId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isNotFound());
    }
}
