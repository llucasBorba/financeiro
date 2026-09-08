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
class CategoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String criarCategoria(Session session, String nome) throws Exception {
        String body = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(nome)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(nome))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    private String criarDespesaNaCategoria(Session session, String categoryId) throws Exception {
        String body = mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "amount": 120.00,
                                  "currency": "BRL",
                                  "description": "mercado",
                                  "dueDate": "2026-05-10",
                                  "type": "VARIABLE"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("conta nova já nasce com as categorias padrão")
    void newAccountIsSeeded() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                // vem ordenado por nome
                .andExpect(jsonPath("$[0].name").value("Alimentação"))
                .andExpect(jsonPath("$[*].active", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    @DisplayName("cria, renomeia e a despesa acompanha o nome novo")
    void createRenameAndExpenseFollows() throws Exception {
        Session session = auth.newUser();
        String categoryId = criarCategoria(session, "Uber");
        criarDespesaNaCategoria(session, categoryId);

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Transporte por app\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Transporte por app"));

        // A despesa continua apontando para o mesmo id — nada foi reescrito nela.
        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Transporte por app"));
    }

    @Test
    @DisplayName("recusa nome repetido com 400")
    void rejectsDuplicateName() throws Exception {
        Session session = auth.newUser();
        criarCategoria(session, "Streaming");

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"streaming\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    @DisplayName("arquivar tira da lista padrão e bloqueia lançamento novo")
    void archivingHidesAndBlocks() throws Exception {
        Session session = auth.newUser();
        String categoryId = criarCategoria(session, "Uber");

        mockMvc.perform(patch("/api/categories/{id}/active", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // some da lista padrão (sobram as 8 semeadas), mas aparece com includeInactive
        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(jsonPath("$.length()").value(8));
        mockMvc.perform(get("/api/categories").param("includeInactive", "true")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(jsonPath("$.length()").value(9));

        // e não aceita despesa nova
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoryId": "%s", "amount": 10.00, "currency": "BRL",
                                  "dueDate": "2026-05-10", "type": "VARIABLE" }
                                """.formatted(categoryId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("excluir categoria em uso devolve 409 sugerindo arquivar")
    void deleteInUseReturns409() throws Exception {
        Session session = auth.newUser();
        String categoryId = criarCategoria(session, "Alimentação especial");
        criarDespesaNaCategoria(session, categoryId);

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("Arquive")));
    }

    @Test
    @DisplayName("exclui categoria sem uso com 204")
    void deleteUnusedReturns204() throws Exception {
        Session session = auth.newUser();
        String categoryId = criarCategoria(session, "Criada por engano");

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("categoria de outro usuário não existe para mim")
    void otherUsersCategoryIsInvisible() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String categoryId = criarCategoria(dono, "Secreta");

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"invadida\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("despesa não aceita categoria de outro usuário nem inexistente")
    void expenseRejectsForeignOrMissingCategory() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String categoriaDoDono = criarCategoria(dono, "Só minha");

        String payload = """
                { "categoryId": "%s", "amount": 10.00, "currency": "BRL",
                  "dueDate": "2026-05-10", "type": "VARIABLE" }
                """;

        // categoria alheia
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.formatted(categoriaDoDono)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Categoria inválida."));

        // categoria inexistente — mesma mensagem, para não revelar quais IDs existem
        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Categoria inválida."));
    }

    @Test
    @DisplayName("exige autenticação")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }
}
