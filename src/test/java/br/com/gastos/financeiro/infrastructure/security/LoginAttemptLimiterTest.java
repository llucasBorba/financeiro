package br.com.gastos.financeiro.infrastructure.security;

import br.com.gastos.financeiro.infrastructure.config.security.LoginAttemptLimiter;
import br.com.gastos.financeiro.infrastructure.config.security.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "lucas@teste.com";
    private static final String IP_ATACANTE = "203.0.113.10";
    private static final String IP_DONO = "198.51.100.20";

    /** Relógio que anda quando o teste manda: 15 minutos passam sem esperar 15 minutos. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-09-09T10:00:00Z");

        void avancar(Duration d) { agora = agora.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private RelogioAjustavel relogio;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        relogio = new RelogioAjustavel();
        limiter = new LoginAttemptLimiter(relogio);
    }

    private void errar(int vezes, String email, String ip) {
        for (int i = 0; i < vezes; i++) {
            limiter.recordFailure(email, ip);
        }
    }

    @Test
    @DisplayName("libera até o quarto erro e trava no quinto")
    void travaNoQuintoErro() {
        errar(LoginAttemptLimiter.MAX_ATTEMPTS - 1, EMAIL, IP_ATACANTE);
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, IP_ATACANTE),
                "quatro erros ainda não travam: gente erra a senha");

        limiter.recordFailure(EMAIL, IP_ATACANTE);
        assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed(EMAIL, IP_ATACANTE));
    }

    @Test
    @DisplayName("TRAVAR NÃO É ARMA: o dono continua entrando do IP dele")
    void travaNaoAtingeODonoEmOutroIP() {
        // Este é o teste que justifica a chave ser o par. Contando só por e-mail, um atacante
        // que saiba o endereço tranca o dono da conta com cinco senhas erradas.
        errar(LoginAttemptLimiter.MAX_ATTEMPTS, EMAIL, IP_ATACANTE);

        assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed(EMAIL, IP_ATACANTE));

        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, IP_DONO),
                "o dono, do IP dele, não pode ser afetado pelo ataque");
    }

    @Test
    @DisplayName("um e-mail travado não afeta outro e-mail da mesma origem")
    void travaNaoVazaEntreEmails() {
        errar(LoginAttemptLimiter.MAX_ATTEMPTS, EMAIL, IP_ATACANTE);

        assertDoesNotThrow(() -> limiter.checkAllowed("outro@teste.com", IP_ATACANTE));
    }

    @Test
    @DisplayName("a trava expira depois de 15 minutos")
    void travaExpira() {
        errar(LoginAttemptLimiter.MAX_ATTEMPTS, EMAIL, IP_ATACANTE);

        relogio.avancar(LoginAttemptLimiter.LOCK_DURATION.minusSeconds(1));
        assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed(EMAIL, IP_ATACANTE));

        relogio.avancar(Duration.ofSeconds(2));
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, IP_ATACANTE));
    }

    @Test
    @DisplayName("informa quanto falta para poder tentar de novo")
    void informaOTempoRestante() {
        errar(LoginAttemptLimiter.MAX_ATTEMPTS, EMAIL, IP_ATACANTE);
        relogio.avancar(Duration.ofMinutes(5));

        TooManyLoginAttemptsException erro = assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed(EMAIL, IP_ATACANTE));

        assertTrue(erro.getRetryAfter().toMinutes() >= 9 && erro.getRetryAfter().toMinutes() <= 10,
                "restavam ~10 minutos, veio " + erro.getRetryAfter());
    }

    @Test
    @DisplayName("acertar a senha zera a contagem")
    void acertoZeraAContagem() {
        // Sem isto, errar duas vezes hoje e três amanhã travaria a conta sem o dono entender.
        errar(LoginAttemptLimiter.MAX_ATTEMPTS - 1, EMAIL, IP_DONO);
        limiter.recordSuccess(EMAIL, IP_DONO);

        errar(LoginAttemptLimiter.MAX_ATTEMPTS - 1, EMAIL, IP_DONO);
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, IP_DONO),
                "a contagem devia ter recomeçado do zero após o acerto");
    }

    @Test
    @DisplayName("erros espaçados além da janela não se acumulam")
    void errosEspacadosNaoAcumulam() {
        errar(LoginAttemptLimiter.MAX_ATTEMPTS - 1, EMAIL, IP_DONO);

        relogio.avancar(LoginAttemptLimiter.LOCK_DURATION.plusMinutes(1));

        limiter.recordFailure(EMAIL, IP_DONO);
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, IP_DONO),
                "a janela reinicia: quatro erros de ontem mais um de hoje não travam");
    }

    @Test
    @DisplayName("a caixa do e-mail não cria uma chave nova")
    void emailNaoDiferenciaMaiusculas() {
        // Senão bastaria alternar entre "Lucas@" e "lucas@" para zerar o contador.
        errar(LoginAttemptLimiter.MAX_ATTEMPTS, "  LUCAS@TESTE.com ", IP_ATACANTE);

        assertThrows(TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed(EMAIL, IP_ATACANTE));
    }
}
