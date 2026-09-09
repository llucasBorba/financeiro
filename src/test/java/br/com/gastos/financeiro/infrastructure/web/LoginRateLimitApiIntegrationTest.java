package br.com.gastos.financeiro.infrastructure.web;

import br.com.gastos.financeiro.support.AuthTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Trava de tentativas no login.
 *
 * <p>Cada teste usa um e-mail próprio porque o limitador é um bean único, com estado, vivo pelo
 * contexto Spring inteiro: e-mails compartilhados fariam um teste travar o outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimitApiIntegrationTest {

    private static final String SENHA = "senha-super-secreta";

    @Autowired
    private MockMvc mockMvc;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    private static String emailNovo() {
        return "limite-" + UUID.randomUUID() + "@teste.com";
    }

    private static RequestPostProcessor origem(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private org.springframework.test.web.servlet.ResultActions tentarLogin(
            String email, String senha, String ip) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .with(origem(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, senha)));
    }

    @Test
    @DisplayName("cinco erros devolvem 401; o sexto devolve 429 com Retry-After")
    void travaNoSextoErro() throws Exception {
        String email = emailNovo();
        auth.newUser(email, SENHA);

        for (int i = 1; i <= 5; i++) {
            tentarLogin(email, "senha-errada-longa", "203.0.113.1")
                    .andExpect(status().isUnauthorized());
        }

        tentarLogin(email, "senha-errada-longa", "203.0.113.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("Tentativas demais"));
    }

    @Test
    @DisplayName("a senha CERTA também é recusada enquanto a trava vale")
    void travaVale_mesmoComASenhaCerta() throws Exception {
        String email = emailNovo();
        auth.newUser(email, SENHA);

        for (int i = 1; i <= 5; i++) {
            tentarLogin(email, "senha-errada-longa", "203.0.113.2");
        }

        // Se a senha certa passasse, a trava não protegeria contra nada.
        tentarLogin(email, SENHA, "203.0.113.2")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("a trava NÃO vira arma: o dono continua entrando do IP dele")
    void naoTrancaODonoDeOutroIP() throws Exception {
        String email = emailNovo();
        auth.newUser(email, SENHA);

        // O atacante martela do IP dele.
        for (int i = 1; i <= 6; i++) {
            tentarLogin(email, "senha-errada-longa", "203.0.113.3");
        }
        tentarLogin(email, SENHA, "203.0.113.3")
                .andExpect(status().isTooManyRequests());

        // E o dono, do IP dele, entra normalmente. É por isso que a chave é o PAR:
        // contando só por e-mail, qualquer um trancaria qualquer conta sabendo o endereço.
        tentarLogin(email, SENHA, "198.51.100.7")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("o 429 é igual para conta inexistente: não revela quais e-mails existem")
    void naoRevelaSeAContaExiste() throws Exception {
        String inexistente = emailNovo();

        for (int i = 1; i <= 5; i++) {
            tentarLogin(inexistente, "qualquer-coisa-longa", "203.0.113.4")
                    .andExpect(status().isUnauthorized());
        }

        // Mesmo status e mesmo corpo que uma conta real travada teria.
        tentarLogin(inexistente, "qualquer-coisa-longa", "203.0.113.4")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Tentativas demais"));
    }

    @Test
    @DisplayName("acertar a senha zera a contagem")
    void acertoZeraAContagem() throws Exception {
        String email = emailNovo();
        auth.newUser(email, SENHA);
        String ip = "203.0.113.5";

        for (int i = 1; i <= 4; i++) {
            tentarLogin(email, "senha-errada-longa", ip).andExpect(status().isUnauthorized());
        }

        tentarLogin(email, SENHA, ip).andExpect(status().isOk());

        // Se a contagem não tivesse zerado, dois erros já bastariam para travar aqui.
        for (int i = 1; i <= 4; i++) {
            tentarLogin(email, "senha-errada-longa", ip).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("travar um e-mail não trava outro da mesma origem")
    void naoVazaEntreEmails() throws Exception {
        String vitima = emailNovo();
        String outro = emailNovo();
        auth.newUser(outro, SENHA);
        String ip = "203.0.113.6";

        for (int i = 1; i <= 6; i++) {
            tentarLogin(vitima, "senha-errada-longa", ip);
        }

        tentarLogin(outro, SENHA, ip).andExpect(status().isOk());
    }
}
