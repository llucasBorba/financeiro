package br.com.gastos.financeiro.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cria usuários de verdade e devolve tokens de verdade para os testes de integração.
 *
 * <p>Poderíamos forjar um JWT direto com o {@code JwtIssuer} e pular o cadastro, mas passar pelo
 * endpoint real faz cada teste exercitar o caminho completo — cadastro, hash de senha, emissão e
 * validação do token. O custo é uma chamada HTTP a mais por teste; o ganho é que uma quebra em
 * qualquer elo dessa corrente aparece.
 */
public final class AuthTestClient {

    /** Um usuário autenticado: o id que o servidor atribuiu e o header pronto para uso. */
    public record Session(UUID userId, String bearer) {}

    private final MockMvc mockMvc;

    public AuthTestClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** Cadastra um usuário novo, com e-mail único, e devolve a sessão dele. */
    public Session newUser() throws Exception {
        return newUser("user-" + UUID.randomUUID() + "@exemplo.com", "senha-super-secreta");
    }

    public Session newUser(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","name":"Usuário de Teste"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new Session(
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                "Bearer " + JsonPath.read(body, "$.accessToken"));
    }

    /** Faz login numa conta existente. */
    public Session login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new Session(
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                "Bearer " + JsonPath.read(body, "$.accessToken"));
    }
}
