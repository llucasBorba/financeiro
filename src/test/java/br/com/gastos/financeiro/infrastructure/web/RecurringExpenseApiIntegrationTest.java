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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecurringExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String categoria(Session s, String nome) throws Exception {
        String body = mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.name == '" + nome + "')].id");
        return ids.get(0);
    }

    private String criarAluguel(Session s, String cat, String valor, int dia,
                                String inicio, String fim) throws Exception {
        String corpoFim = fim != null ? "\"endMonth\": \"%s\",".formatted(fim) : "";
        String body = mockMvc.perform(post("/api/recurring-expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "amount": %s,
                                  "currency": "BRL",
                                  "description": "Aluguel",
                                  "dayOfMonth": %d,
                                  %s
                                  "startMonth": "%s"
                                }
                                """.formatted(cat, valor, dia, corpoFim, inicio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Aluguel"))
                .andExpect(jsonPath("$.categoryName").value("Moradia"))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("criar a recorrência já cria as despesas do período")
    void createGeneratesExpenses() throws Exception {
        Session s = auth.newUser();
        String moradia = categoria(s, "Moradia");
        criarAluguel(s, moradia, "1500.00", 10, "2027-01", "2027-03");

        mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2027-01-01").param("endDate", "2027-12-31")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].recurringExpenseId").isNotEmpty())
                .andExpect(jsonPath("$[0].categoryName").value("Moradia"));
    }

    @Test
    @DisplayName("vencimento 31 gruda no último dia de cada mês")
    void day31ClampsToMonthEnd() throws Exception {
        Session s = auth.newUser();
        String moradia = categoria(s, "Moradia");
        criarAluguel(s, moradia, "100.00", 31, "2027-01", "2027-04");

        mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2027-01-01").param("endDate", "2027-04-30")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$[?(@.dueDate == '2027-01-31')]").exists())
                .andExpect(jsonPath("$[?(@.dueDate == '2027-02-28')]").exists())
                .andExpect(jsonPath("$[?(@.dueDate == '2027-03-31')]").exists())
                .andExpect(jsonPath("$[?(@.dueDate == '2027-04-30')]").exists());
    }

    @Test
    @DisplayName("estender o horizonte é idempotente")
    void generateIsIdempotent() throws Exception {
        Session s = auth.newUser();
        String moradia = categoria(s, "Moradia");
        String id = criarAluguel(s, moradia, "1500.00", 10, "2027-01", "2027-03");

        mockMvc.perform(post("/api/recurring-expenses/{id}/generate", id)
                        .param("through", "2027-06")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                // o modelo termina em março; nada além disso é criado
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("editar atualiza as futuras e preserva a paga")
    void updateKeepsPaidUntouched() throws Exception {
        Session s = auth.newUser();
        String moradia = categoria(s, "Moradia");
        String id = criarAluguel(s, moradia, "1500.00", 10, "2027-01", "2027-03");

        String lista = mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2027-01-01").param("endDate", "2027-01-31")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andReturn().getResponse().getContentAsString();
        String janeiro = JsonPath.read(lista, "$[0].id");

        mockMvc.perform(patch("/api/expenses/{id}/payment", janeiro)
                .header(HttpHeaders.AUTHORIZATION, s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/recurring-expenses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoryId": "%s", "amount": 1600.00, "currency": "BRL",
                                  "description": "Aluguel reajustado", "dayOfMonth": 10,
                                  "endMonth": "2027-03" }
                                """.formatted(moradia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1600.00));

        // janeiro (paga) continua 1500
        mockMvc.perform(get("/api/expenses/{id}", janeiro).header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.status").value("PAID"));

        // fevereiro e março (pendentes e futuras) foram para 1600
        mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2027-02-01").param("endDate", "2027-03-31")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$[0].amount").value(1600.00))
                .andExpect(jsonPath("$[1].amount").value(1600.00));
    }

    @Test
    @DisplayName("apagar o modelo remove as futuras e mantém a paga")
    void deleteKeepsPaidHistory() throws Exception {
        Session s = auth.newUser();
        String moradia = categoria(s, "Moradia");
        String id = criarAluguel(s, moradia, "1500.00", 10, "2027-01", "2027-03");

        String lista = mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2027-01-01").param("endDate", "2027-01-31")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andReturn().getResponse().getContentAsString();
        String janeiro = JsonPath.read(lista, "$[0].id");
        mockMvc.perform(patch("/api/expenses/{id}/payment", janeiro)
                .header(HttpHeaders.AUTHORIZATION, s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        mockMvc.perform(delete("/api/recurring-expenses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recurring-expenses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isNotFound());

        // a paga sobrevive, agora como lançamento avulso
        mockMvc.perform(get("/api/expenses/{id}", janeiro).header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurringExpenseId").doesNotExist());

        mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2027-01-01").param("endDate", "2027-12-31")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("recorrência de outro usuário não existe para mim")
    void isolatesUsers() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String id = criarAluguel(dono, categoria(dono, "Moradia"), "1500.00", 10, "2027-01", "2027-02");

        mockMvc.perform(get("/api/recurring-expenses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/recurring-expenses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recurring-expenses").header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("valida dia fora do intervalo")
    void validatesDayOfMonth() throws Exception {
        Session s = auth.newUser();

        mockMvc.perform(post("/api/recurring-expenses")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoryId": "%s", "amount": 100.00, "description": "x",
                                  "dayOfMonth": 32, "startMonth": "2027-01" }
                                """.formatted(categoria(s, "Moradia"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dayOfMonth").isNotEmpty());
    }

    @Test
    @DisplayName("exige autenticação")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/recurring-expenses")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("geração com alvo absurdo devolve 400 e a aplicação segue de pé")
    void absurdGenerationTargetIsRejected() throws Exception {
        Session s = auth.newUser();
        String id = criarAluguel(s, categoria(s, "Moradia"), "1500.00", 10, "2027-01", null);

        // Antes da guarda no domínio, esta chamada montava uma lista sem teto e derrubava a JVM
        // — para todos os usuários, não só para quem chamou.
        mockMvc.perform(post("/api/recurring-expenses/{id}/generate", id)
                        .param("through", "2200-12")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        // a aplicação continua atendendo normalmente
        mockMvc.perform(get("/api/recurring-expenses/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("estender dentro do teto continua funcionando")
    void reasonableGenerationStillWorks() throws Exception {
        Session s = auth.newUser();
        String id = criarAluguel(s, categoria(s, "Moradia"), "1500.00", 10, "2027-01", null);

        // criação já gerou 12; estender para 24 meses cria os 12 restantes
        mockMvc.perform(post("/api/recurring-expenses/{id}/generate", id)
                        .param("through", "2028-12")
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }
}
