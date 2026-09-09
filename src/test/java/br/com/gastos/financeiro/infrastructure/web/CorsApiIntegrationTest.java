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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O front do Next roda em localhost:3000 e a API em 8080 — origens diferentes, porque a porta
 * entra na conta. Sem CORS o navegador barra a chamada antes de ela sair, e nada aparece no log
 * do servidor.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsApiIntegrationTest {

    private static final String FRONT = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    @Test
    @DisplayName("preflight do front é aprovado, com o header Authorization liberado")
    void preflightAprovado() throws Exception {
        mockMvc.perform(options("/api/expenses")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        // Sem Authorization em allowedHeaders, o preflight passaria e a
                        // requisição real seria barrada — sintoma confuso.
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().stringValues(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsStringIgnoringCase("authorization"))));
    }

    @Test
    @DisplayName("requisição autenticada de verdade volta com o header de origem liberada")
    void requisicaoRealLiberada() throws Exception {
        Session s = auth.newUser();

        mockMvc.perform(get("/api/expenses")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.AUTHORIZATION, s.bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @Test
    @DisplayName("expõe Location e Retry-After, que o JavaScript não enxergaria sozinho")
    void exponeOsHeadersQueOFrontPrecisa() throws Exception {
        // O navegador esconde do JS todo header de resposta fora de uma lista curta. Location
        // é devolvido pelos POST de criação; Retry-After diz quando sair da trava de login.
        mockMvc.perform(options("/api/expenses")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsString("Retry-After"))));
    }

    @Test
    @DisplayName("origem não configurada é recusada")
    void origemDesconhecidaERecusada() throws Exception {
        // Se qualquer site pudesse chamar a API com o token do usuário, o CORS não estaria
        // protegendo nada.
        mockMvc.perform(options("/api/expenses")
                        .header(HttpHeaders.ORIGIN, "https://site-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("as rotas públicas de auth também respondem ao front")
    void authTambemLiberada() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }
}
