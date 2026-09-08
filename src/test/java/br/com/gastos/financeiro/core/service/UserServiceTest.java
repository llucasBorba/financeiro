package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.AuthenticationException;
import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.model.Email;
import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateUserUseCase.LoginCommand;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateWithGoogleUseCase.GoogleLoginCommand;
import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase.RegisterCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private static final String SENHA = "senha-super-secreta";

    private InMemoryUserRepository repository;
    private FakePasswordHasher hasher;
    private UserService service;
    private InMemoryCategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        hasher = new FakePasswordHasher();
        categoryRepository = new InMemoryCategoryRepository();
        service = new UserService(repository, hasher, categoryRepository);
    }

    // ---------- cadastro ----------

    @Test
    @DisplayName("cadastra guardando o hash da senha, nunca a senha em claro")
    void registersHashingThePassword() {
        User user = service.execute(new RegisterCommand("Lucas@Exemplo.com", SENHA, "Lucas"));

        assertEquals("lucas@exemplo.com", user.getEmail().value());
        assertTrue(user.hasPassword());
        assertNotNull(user.getPasswordHash());
        assertFalse(user.getPasswordHash().contains(SENHA) && user.getPasswordHash().equals(SENHA),
                "o hash não pode ser a própria senha");
        // Conta por senha nasce sem verificação de e-mail: ninguém confirmou o endereço ainda.
        assertFalse(user.isEmailVerified());
    }

    @Test
    @DisplayName("recusa cadastro com e-mail já usado, mesmo em outra caixa")
    void rejectsDuplicateEmail() {
        service.execute(new RegisterCommand("lucas@exemplo.com", SENHA, "Lucas"));

        assertThrows(BusinessException.class,
                () -> service.execute(new RegisterCommand("LUCAS@exemplo.com", "outra-senha-longa", "Outro")));
    }

    @Test
    @DisplayName("recusa senha curta demais")
    void rejectsShortPassword() {
        assertThrows(BusinessException.class,
                () -> service.execute(new RegisterCommand("lucas@exemplo.com", "1234567", "Lucas")));
    }

    // ---------- login por senha ----------

    @Test
    @DisplayName("autentica com a senha correta")
    void authenticates() {
        User cadastrado = service.execute(new RegisterCommand("lucas@exemplo.com", SENHA, "Lucas"));

        User logado = service.execute(new LoginCommand("LUCAS@Exemplo.com", SENHA));

        assertEquals(cadastrado.getId(), logado.getId());
    }

    @Test
    @DisplayName("dá a mesma resposta para senha errada e para e-mail inexistente")
    void doesNotRevealWhichEmailsExist() {
        service.execute(new RegisterCommand("lucas@exemplo.com", SENHA, "Lucas"));

        AuthenticationException senhaErrada = assertThrows(AuthenticationException.class,
                () -> service.execute(new LoginCommand("lucas@exemplo.com", "senha-errada-longa")));
        AuthenticationException naoExiste = assertThrows(AuthenticationException.class,
                () -> service.execute(new LoginCommand("ninguem@exemplo.com", SENHA)));

        // Mensagens diferentes entregariam ao atacante a lista de e-mails cadastrados.
        assertEquals(senhaErrada.getMessage(), naoExiste.getMessage());
    }

    @Test
    @DisplayName("gasta o mesmo trabalho mesmo quando o e-mail não existe")
    void spendsTheSameEffortForUnknownEmails() {
        // Sair mais cedo quando o usuário não existe deixaria a resposta mensuravelmente
        // mais rápida — um cronômetro bastaria para descobrir quais e-mails estão cadastrados.
        assertThrows(AuthenticationException.class,
                () -> service.execute(new LoginCommand("ninguem@exemplo.com", SENHA)));

        assertEquals(1, hasher.matchCalls(), "a comparação de senha precisa rodar mesmo sem usuário");
    }

    @Test
    @DisplayName("e-mail malformado no login é apenas credencial inválida")
    void malformedEmailIsJustInvalidCredentials() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> service.execute(new LoginCommand("isso-nao-e-email", SENHA)));

        assertEquals("E-mail ou senha inválidos.", ex.getMessage());
    }

    @Test
    @DisplayName("conta só do Google não entra por senha")
    void googleOnlyAccountCannotUsePassword() {
        service.execute(new GoogleLoginCommand("google-123", "lucas@exemplo.com", "Lucas", true));

        assertThrows(AuthenticationException.class,
                () -> service.execute(new LoginCommand("lucas@exemplo.com", SENHA)));
    }

    // ---------- login com Google ----------

    @Test
    @DisplayName("primeiro login com Google cria a conta já verificada")
    void createsAccountOnFirstGoogleLogin() {
        User user = service.execute(new GoogleLoginCommand("google-123", "Lucas@Exemplo.com", "Lucas", true));

        assertEquals("lucas@exemplo.com", user.getEmail().value());
        assertEquals("google-123", user.getGoogleId());
        assertFalse(user.hasPassword());
        assertTrue(user.isEmailVerified());
    }

    @Test
    @DisplayName("logins seguintes reaproveitam a mesma conta")
    void reusesAccountOnSubsequentLogins() {
        User primeiro = service.execute(new GoogleLoginCommand("google-123", "lucas@exemplo.com", "Lucas", true));
        User segundo = service.execute(new GoogleLoginCommand("google-123", "lucas@exemplo.com", "Lucas", true));

        assertEquals(primeiro.getId(), segundo.getId());
    }

    @Test
    @DisplayName("vincula ao usuário existente quando o Google confirma o e-mail")
    void linksToExistingAccount() {
        User porSenha = service.execute(new RegisterCommand("lucas@exemplo.com", SENHA, "Lucas"));

        User porGoogle = service.execute(
                new GoogleLoginCommand("google-123", "lucas@exemplo.com", "Lucas", true));

        // Mesma pessoa, mesma conta — e agora ela entra pelos dois caminhos.
        assertEquals(porSenha.getId(), porGoogle.getId());
        assertTrue(porGoogle.hasPassword());
        assertEquals("google-123", porGoogle.getGoogleId());
        assertTrue(porGoogle.isEmailVerified());
    }

    @Test
    @DisplayName("recusa vincular quando o Google não confirmou o e-mail")
    void refusesToLinkUnverifiedEmail() {
        service.execute(new RegisterCommand("lucas@exemplo.com", SENHA, "Lucas"));

        // Sem a confirmação do Google não há nada ligando quem está logando à conta existente.
        assertThrows(AuthenticationException.class,
                () -> service.execute(new GoogleLoginCommand("google-123", "lucas@exemplo.com", "Lucas", false)));
    }

    @Test
    @DisplayName("não rouba uma conta já vinculada a outra conta Google")
    void refusesToStealLinkedAccount() {
        service.execute(new GoogleLoginCommand("google-123", "lucas@exemplo.com", "Lucas", true));

        assertThrows(IllegalStateException.class,
                () -> service.execute(new GoogleLoginCommand("google-999", "lucas@exemplo.com", "Lucas", true)));
    }
}
