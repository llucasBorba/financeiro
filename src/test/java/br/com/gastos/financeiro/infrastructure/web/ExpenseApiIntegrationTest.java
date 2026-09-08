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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a API de despesas de ponta a ponta: token -> controller -> use case -> adapter -> banco.
 *
 * <p>Cada teste cadastra um usuário de verdade e usa o token que o servidor emitiu. Não há atalho
 * de identidade: é exatamente o caminho que um cliente real percorre.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    /** Pega o id de uma das 8 categorias semeadas na conta — categoria virou obrigatória. */
    private String umaCategoria(Session session) throws Exception {
        String body = mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private String criarDespesa(Session session) throws Exception {
        String payload = """
                {
                  "categoryId": "%s",
                  "amount": 250.75,
                  "currency": "BRL",
                  "description": "Conta de energia",
                  "dueDate": "2026-05-10"
                }
                """.formatted(umaCategoria(session));

        String body = mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.userId").value(session.userId().toString()))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("cria, consulta e paga uma despesa")
    void fullExpenseFlow() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(get("/api/expenses/{id}", expenseId).header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Conta de energia"));

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    @DisplayName("retorna 409 ao pagar duas vezes a mesma despesa")
    void conflictOnDoublePayment() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                .header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta despesa já foi paga."));
    }

    @Test
    @DisplayName("retorna 404 para despesa inexistente")
    void notFound() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/api/expenses/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("retorna 400 detalhando os campos inválidos")
    void validationErrors() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": -5, "dueDate": "2026-05-10" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isNotEmpty());
    }


    @Test
    @DisplayName("para outro usuário, a despesa simplesmente não existe (404, não 403)")
    void hidesExpenseFromOtherUser() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String expenseId = criarDespesa(dono);

        // 404 e não 403: um "acesso negado" confirmaria que este ID existe.
        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("recusa acesso sem token")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("recusa token adulterado")
    void rejectsTamperedToken() throws Exception {
        Session session = auth.newUser();
        String adulterado = session.bearer() + "x";   // quebra a assinatura

        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT substitui os dados e mantém o pagamento")
    void updateKeepsPaymentState() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                .header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "amount": 99.90,
                                  "currency": "BRL",
                                  "description": "Conta de energia (corrigida)",
                                  "dueDate": "2026-06-15"
                                }
                                """.formatted(umaCategoria(session))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(99.90))
                .andExpect(jsonPath("$.description").value("Conta de energia (corrigida)"))
                .andExpect(jsonPath("$.status").value("PAID"))          // não despagou
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }

    @Test
    @DisplayName("PUT valida os campos como na criação")
    void updateValidatesFields() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": -1, "dueDate": "2026-06-15" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isNotEmpty());
    }

    @Test
    @DisplayName("DELETE remove a despesa e devolve 204")
    void deleteRemovesExpense() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("outro usuário não edita nem apaga: 404 e o dado fica intacto")
    void otherUserCannotUpdateOrDelete() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String expenseId = criarDespesa(dono);

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoryId": "%s", "amount": 1.00, "currency": "BRL", "description": "invadido",
                                  "dueDate": "2026-06-15" }
                                """.formatted(umaCategoria(intruso))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());

        // o dono continua com a despesa original
        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, dono.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Conta de energia"));
    }

    // ---------- compra à vista ----------

    @Test
    @DisplayName("uma chamada só registra uma compra já paga")
    void createsAlreadyPaidInASingleCall() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "amount": 120.00,
                                  "currency": "BRL",
                                  "description": "Mercado",
                                  "dueDate": "2026-09-02",
                                  "paidAt": "2026-09-02T14:30:00"
                                }
                                """.formatted(umaCategoria(session))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").value("2026-09-02T14:30:00"));
    }

    @Test
    @DisplayName("sem paidAt a despesa continua nascendo pendente")
    void stillCreatesPendingWithoutPaidAt() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paidAt").doesNotExist());
    }

    // ---------- desfazer pagamento ----------

    @Test
    @DisplayName("DELETE em /payment devolve a despesa para 'a pagar'")
    void undoesPayment() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                .header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/expenses/{id}/payment", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paidAt").doesNotExist());

        // a despesa em si continua existindo — só o pagamento foi removido
        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Conta de energia"));
    }

    @Test
    @DisplayName("desfazer pagamento de despesa não paga devolve 409")
    void undoOnUnpaidReturns409() throws Exception {
        Session session = auth.newUser();
        String expenseId = criarDespesa(session);

        mockMvc.perform(delete("/api/expenses/{id}/payment", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta despesa não está paga."));
    }

    @Test
    @DisplayName("outro usuário não desfaz pagamento alheio")
    void otherUserCannotUndoPayment() throws Exception {
        Session dono = auth.newUser();
        Session intruso = auth.newUser();
        String expenseId = criarDespesa(dono);

        mockMvc.perform(patch("/api/expenses/{id}/payment", expenseId)
                .header(HttpHeaders.AUTHORIZATION, dono.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        mockMvc.perform(delete("/api/expenses/{id}/payment", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, intruso.bearer()))
                .andExpect(status().isNotFound());
    }
}
