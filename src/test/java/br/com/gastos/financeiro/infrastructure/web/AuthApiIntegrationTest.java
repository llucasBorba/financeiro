package br.com.gastos.financeiro.infrastructure.web;

import br.com.gastos.financeiro.support.AuthTestClient;
import br.com.gastos.financeiro.support.AuthTestClient.Session;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    private static final String SENHA = "senha-super-secreta";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private String emailNovo() {
        return "user-" + UUID.randomUUID() + "@exemplo.com";
    }

    private String registerPayload(String email, String password) {
        return """
                {"email":"%s","password":"%s","name":"Lucas"}
                """.formatted(email, password);
    }

    @Test
    @DisplayName("cadastro devolve token e já autentica nas rotas protegidas")
    void registerReturnsUsableToken() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cadastro devolve os dados da conta sem nunca expor o hash da senha")
    void neverExposesPasswordHash() throws Exception {
        String email = emailNovo();

        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.hasPassword").value(true))
                .andExpect(jsonPath("$.user.googleLinked").value(false))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Nem o hash nem a senha podem aparecer em lugar nenhum da resposta.
        assertFalse(body.contains(SENHA), "a senha não pode voltar na resposta");
        assertFalse(body.contains("$2a$"), "o hash BCrypt não pode voltar na resposta");
    }

    @Test
    @DisplayName("recusa cadastro com e-mail repetido")
    void rejectsDuplicateEmail() throws Exception {
        String email = emailNovo();
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload(email, SENHA))).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, SENHA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("recusa cadastro com senha curta ou e-mail inválido")
    void rejectsWeakRegistration() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(emailNovo(), "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isNotEmpty());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("nao-e-email", SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    @Test
    @DisplayName("login com a senha correta devolve um token novo")
    void loginWorks() throws Exception {
        String email = emailNovo();
        Session cadastro = auth.newUser(email, SENHA);
        Session login = auth.login(email, SENHA);

        org.junit.jupiter.api.Assertions.assertEquals(cadastro.userId(), login.userId());
    }

    @Test
    @DisplayName("senha errada e e-mail inexistente devolvem 401 com a mesma mensagem")
    void loginFailuresAreIndistinguishable() throws Exception {
        String email = emailNovo();
        auth.newUser(email, SENHA);

        String detalheSenhaErrada = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"senha-errada-longa\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String detalheNaoExiste = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(emailNovo(), SENHA)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(detalheSenhaErrada, detalheNaoExiste,
                "respostas diferentes revelariam quais e-mails estão cadastrados");
    }

    @Test
    @DisplayName("/auth/me devolve o dono do token")
    void meReturnsTokenOwner() throws Exception {
        Session session = auth.newUser();

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.userId().toString()));
    }

    @Test
    @DisplayName("/auth/me exige token")
    void meRequiresToken() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("recusa id_token do Google que não veio do Google")
    void rejectsForgedGoogleToken() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMjMifQ.\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("todo 401 sai no mesmo formato, sem dizer qual checagem falhou")
    void unauthorizedResponsesAreUniform() throws Exception {
        // Sem token
        String semToken = mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andReturn().getResponse().getContentAsString();

        // Token com assinatura quebrada
        var comTokenRuim = mockMvc.perform(get("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.assinatura-invalida"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andReturn().getResponse();

        org.junit.jupiter.api.Assertions.assertEquals(semToken, comTokenRuim.getContentAsString(),
                "respostas diferentes revelariam qual checagem falhou");

        // O WWW-Authenticate não pode carregar o diagnóstico do Spring Security.
        String challenge = comTokenRuim.getHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertFalse(challenge != null && challenge.contains("error_description"),
                "o header não deve dizer qual parte do token falhou: " + challenge);
    }

    @Test
    @DisplayName("recusa corpo vazio no login com Google")
    void rejectsEmptyGoogleToken() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("troca a senha estando logado: a antiga para de valer e a nova entra")
    void trocaSenhaEstandoLogado() throws Exception {
        String email = emailNovo();
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, SENHA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");

        mockMvc.perform(post("/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"%s\",\"newPassword\":\"senha-nova-do-lucas\"}"
                                .formatted(SENHA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, SENHA)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"senha-nova-do-lucas\"}".formatted(email))) 
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("trocar senha exige estar autenticado")
    void trocaSenhaExigeToken() throws Exception {
        mockMvc.perform(post("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"a\",\"newPassword\":\"senha-nova-do-lucas\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("senha atual errada devolve 400 e nao troca nada")
    void senhaAtualErradaNaoTroca() throws Exception {
        String email = emailNovo();
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, SENHA)))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");

        mockMvc.perform(post("/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"chute-errado-999\",\"newPassword\":\"senha-nova-do-lucas\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, SENHA)))
                .andExpect(status().isOk());
    }
}
