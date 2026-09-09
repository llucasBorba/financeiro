package br.com.gastos.financeiro.infrastructure.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava tentativas de login repetidas.
 *
 * <p><b>A chave é o par (e-mail, origem), não o e-mail sozinho.</b> Essa é a decisão que faz a
 * diferença entre proteção e arma. Contando só por e-mail, qualquer pessoa que saiba o seu
 * endereço tranca você para fora da sua própria conta com três senhas erradas — e repete a cada
 * expiração, indefinidamente. Nunca precisaria descobrir a senha. É por isso que bloqueio puro
 * por conta é desaconselhado pelo OWASP e pelo NIST SP 800-63B.
 *
 * <p>Com o par, quem ataca do IP dele tranca apenas {@code (seu-email, IP-dele)}: você, do seu
 * IP, continua entrando. E quem tenta adivinhar sua senha da máquina dele é barrado no quinto
 * erro, que é o objetivo.
 *
 * <p>O contador sobe <b>antes</b> de qualquer consulta ao banco, e por isso não revela nada:
 * como o login roda o bcrypt mesmo para e-mail inexistente (ver {@code UserService}), a
 * contagem e o 429 são idênticos exista ou não a conta. Se o limite só valesse para contas
 * reais, o próprio 429 viraria um jeito de descobrir quais e-mails estão cadastrados,
 * desfazendo o cuidado que o login já tem.
 *
 * <p>O estado é em memória, o que é suficiente para uma instância só. Duas limitações
 * assumidas: reinicia zerado num deploy — um atacante não ganha nada esperando —, e com duas
 * instâncias cada uma teria seu contador. Trocar por um armazenamento compartilhado é mudar
 * esta classe, não o desenho.
 *
 * <p>O que esta versão NÃO cobre, de propósito: um atacante mandando e-mails aleatórios de um
 * IP só gera uma chave nova a cada tentativa, então o contador nunca dispara e o bcrypt roda a
 * cada requisição (~76 ms, ~13/s saturam um núcleo). Cobrir isso exigiria um segundo contador
 * por IP. Foi deixado de fora por ser um ataque que exige alguém mirando o sistema de
 * propósito, sem indício de que aconteça; se aparecer no log, é um contador a mais nesta mesma
 * classe.
 */
@Component
public class LoginAttemptLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptLimiter.class);

    /**
     * Cinco, e não três. Gente erra a senha por caps lock, por autocomplete velho ou por
     * confundir com a senha de outro site. Quem está adivinhando não se importa com a
     * diferença entre 3 e 5; o dono da conta se importa muito quando trava sozinho.
     */
    public static final int MAX_ATTEMPTS = 5;

    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /**
     * Teto de chaves guardadas. Sem ele, tentativas com e-mails aleatórios criariam uma entrada
     * nova cada uma e o mapa cresceria sem limite — a proteção viraria um vazamento de memória.
     */
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    /** O relógio é injetado para que o teste consiga avançar 15 minutos sem esperar 15 minutos. */
    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Recusa a tentativa se a chave já estourou o limite e ainda está no prazo de espera.
     *
     * @throws TooManyLoginAttemptsException quando está travado
     */
    public void checkAllowed(String email, String remoteAddress) {
        Attempts atual = attempts.get(key(email, remoteAddress));
        if (atual == null) {
            return;
        }

        Duration restante = atual.remainingLock(clock.instant());
        if (!restante.isZero()) {
            throw new TooManyLoginAttemptsException(restante);
        }
    }

    /** Registra um erro. No {@link #MAX_ATTEMPTS}-ésimo, a chave passa a ser recusada. */
    public void recordFailure(String email, String remoteAddress) {
        purgeIfCrowded();

        String chave = key(email, remoteAddress);
        Attempts atualizado = attempts.compute(chave,
                (k, anterior) -> anterior == null || anterior.expired(clock.instant())
                        ? new Attempts(1, clock.instant())
                        : anterior.increment(clock.instant()));

        if (atualizado.count() == MAX_ATTEMPTS) {
            // Sem o e-mail no log: registrar a tentativa junto do endereço transformaria o log
            // numa lista de e-mails testados por quem atacou.
            log.warn("Login bloqueado por excesso de tentativas a partir de {} por {}",
                    remoteAddress, LOCK_DURATION);
        }
    }

    /**
     * Zera a contagem depois de um acerto.
     *
     * <p>Sem isto, errar duas vezes hoje e uma amanhã acabaria travando a conta sem que o dono
     * entendesse por quê — os erros de meses diferentes se somariam.
     */
    public void recordSuccess(String email, String remoteAddress) {
        attempts.remove(key(email, remoteAddress));
    }

    /**
     * O e-mail é normalizado igual ao domínio faz, para que "Lucas@X.com" e "lucas@x.com"
     * caiam na mesma chave — senão variar a caixa das letras zeraria o contador.
     */
    private static String key(String email, String remoteAddress) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalizado + "|" + remoteAddress;
    }

    private void purgeIfCrowded() {
        if (attempts.size() < MAX_TRACKED_KEYS) {
            return;
        }
        Instant agora = clock.instant();
        attempts.values().removeIf(a -> a.expired(agora));
    }

    /**
     * @param count      erros acumulados
     * @param lastFailure instante do último erro, de onde conta a janela
     */
    private record Attempts(int count, Instant lastFailure) {

        Attempts increment(Instant agora) {
            return new Attempts(count + 1, agora);
        }

        /** A janela reinicia sozinha: passados 15 minutos do último erro, a contagem some. */
        boolean expired(Instant agora) {
            return !agora.isBefore(lastFailure.plus(LOCK_DURATION));
        }

        /** Zero quando não está travado. */
        Duration remainingLock(Instant agora) {
            if (count < MAX_ATTEMPTS || expired(agora)) {
                return Duration.ZERO;
            }
            return Duration.between(agora, lastFailure.plus(LOCK_DURATION));
        }
    }
}
