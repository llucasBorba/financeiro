package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateWithGoogleUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateWithGoogleUseCase.GoogleLoginCommand;
import br.com.gastos.financeiro.core.ports.ingoing.ChangePasswordUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.config.security.LoginAttemptLimiter;
import br.com.gastos.financeiro.infrastructure.config.security.GoogleTokenVerifier;
import br.com.gastos.financeiro.infrastructure.config.security.GoogleTokenVerifier.GoogleAccount;
import br.com.gastos.financeiro.infrastructure.config.security.JwtIssuer;
import br.com.gastos.financeiro.infrastructure.web.dto.AuthResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.ChangePasswordRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.GoogleLoginRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.LoginRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.RegisterRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Os três caminhos de entrada no sistema. Todos terminam do mesmo jeito: um usuário do domínio
 * vira um token nosso.
 *
 * <p>Repare que a emissão do token mora aqui, na infraestrutura, e não no {@code UserService}.
 * O domínio responde "quem é esta pessoa?"; formato de credencial é assunto da borda.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final AuthenticateWithGoogleUseCase authenticateWithGoogle;
    private final FindUserUseCase findUser;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtIssuer jwtIssuer;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final ChangePasswordUseCase changePassword;

    public AuthController(RegisterUserUseCase registerUser,
                          AuthenticateUserUseCase authenticateUser,
                          AuthenticateWithGoogleUseCase authenticateWithGoogle,
                          FindUserUseCase findUser,
                          GoogleTokenVerifier googleTokenVerifier,
                          JwtIssuer jwtIssuer,
                          LoginAttemptLimiter loginAttemptLimiter,
                          ChangePasswordUseCase changePassword) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.authenticateWithGoogle = authenticateWithGoogle;
        this.findUser = findUser;
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtIssuer = jwtIssuer;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.changePassword = changePassword;
    }

    /** Cadastro por e-mail e senha. Já devolve o token, para o usuário não precisar logar em seguida. */
    @Operation(summary = "Cria conta com e-mail e senha",
            description = "Devolve o token já emitido, para não exigir um login logo em seguida. A conta nasce com 8 categorias padrão, editáveis.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUser.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenFor(user));
    }

    /**
     * A checagem do limitador vem ANTES de {@code authenticateUser}, e isso é o ponto: o login
     * roda bcrypt (~76 ms) mesmo para e-mail inexistente, para não vazar quais contas existem
     * pelo tempo de resposta. Se o limite fosse conferido depois, ele contaria as tentativas
     * mas não evitaria o custo — e martelar esta rota continuaria consumindo CPU.
     */
    @Operation(summary = "Autentica com e-mail e senha",
            description = "Qualquer falha devolve a mesma resposta, seja e-mail inexistente ou senha errada: respostas distintas revelariam quais e-mails estão cadastrados. "
                    + "Após 5 erros da mesma origem para o mesmo e-mail, responde 429 por 15 minutos; o cabeçalho Retry-After diz quando tentar de novo.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String origem = httpRequest.getRemoteAddr();

        loginAttemptLimiter.checkAllowed(request.email(), origem);

        User user;
        try {
            user = authenticateUser.execute(request.toCommand());
        } catch (RuntimeException falha) {
            loginAttemptLimiter.recordFailure(request.email(), origem);
            throw falha;
        }

        loginAttemptLimiter.recordSuccess(request.email(), origem);
        return ResponseEntity.ok(tokenFor(user));
    }

    /**
     * Login com Google. O front obtém o {@code id_token} no navegador e manda para cá; a
     * verificação de assinatura, emissor e audiência acontece no servidor — confiar na
     * validação feita pelo cliente seria não validar nada.
     */
    @Operation(summary = "Entra com a conta Google",
            description = "Recebe o id_token que o front obteve do Google. Assinatura, emissor e audiência são conferidos no servidor — confiar na validação feita pelo cliente seria não validar. Primeiro acesso cria a conta; acessos seguintes reaproveitam a mesma.")
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleAccount account = googleTokenVerifier.verify(request.idToken());

        User user = authenticateWithGoogle.execute(new GoogleLoginCommand(
                account.googleId(), account.email(), account.name(), account.emailVerified()));

        return ResponseEntity.ok(tokenFor(user));
    }

    /** Quem sou eu — útil para o front reidratar a sessão a partir de um token guardado. */
    @Operation(summary = "Dados do usuário autenticado",
            description = "Útil para o front reidratar a sessão a partir de um token guardado.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@CurrentUser UUID userId) {
        return ResponseEntity.ok(UserResponse.from(findUser.findById(userId)));
    }

    /**
     * Troca a senha de quem já está logado.
     *
     * <p>Não depende de e-mail: quem está autenticado já provou quem é, e a senha atual é a
     * prova para esta operação específica. É o caminho que existe hoje para quem desconfia que
     * a senha vazou.
     */
    @Operation(summary = "Troca a própria senha",
            description = "Exige a senha atual como prova. Não desloga as sessões já abertas: "
                    + "um token emitido antes continua válido até expirar, porque JWT não é revogável.")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@CurrentUser UUID userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        changePassword.execute(request.toCommand(userId));
        return ResponseEntity.noContent().build();
    }

    private AuthResponse tokenFor(User user) {
        return AuthResponse.of(jwtIssuer.issue(user), user);
    }
}
