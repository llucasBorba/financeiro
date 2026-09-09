package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.AuthenticationException;
import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Email;
import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateWithGoogleUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.ingoing.ChangePasswordUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.PasswordHasherPort;
import br.com.gastos.financeiro.core.ports.outgoing.UserRepositoryPort;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UserService implements RegisterUserUseCase, AuthenticateUserUseCase,
        AuthenticateWithGoogleUseCase, ChangePasswordUseCase, FindUserUseCase {

    /** Tamanho mínimo da senha. Comprimento é a defesa que mais importa contra força bruta. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Mensagem única para qualquer falha de login.
     *
     * <p>Dizer "e-mail não cadastrado" versus "senha incorreta" entrega ao atacante uma lista
     * de quais e-mails existem no sistema (<em>user enumeration</em>). O usuário legítimo perde
     * pouco; o atacante perde muito.
     */
    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos.";

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final CategoryRepositoryPort categoryRepository;

    public UserService(UserRepositoryPort userRepository,
                       PasswordHasherPort passwordHasher,
                       CategoryRepositoryPort categoryRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "O repositório de usuários é obrigatório.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "O hasher de senha é obrigatório.");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "O repositório de categorias é obrigatório.");
    }

    @Override
    public void execute(ChangePasswordCommand command) {
        User user = findById(command.userId());

        // Antes de conferir a senha atual: numa conta só do Google não existe hash, e
        // matches(senha, null) devolve falso — o usuário receberia "senha atual incorreta"
        // para uma conta que nunca teve senha. Aqui não há risco de vazar nada, porque a
        // pessoa já está autenticada como ela mesma.
        if (!user.hasPassword()) {
            throw new BusinessException("Esta conta entra pelo Google e não tem senha para trocar.");
        }

        // A senha ATUAL é a prova. Sem ela, um token roubado viraria acesso permanente: o
        // ladrão trocaria a senha, continuaria entrando depois de o token expirar, e ainda
        // deixaria o dono de fora da própria conta.
        if (!passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("A senha atual está incorreta.");
        }

        validatePassword(command.newPassword());

        // Recusar a senha repetida evita a troca que parece ter acontecido e não aconteceu:
        // quem troca a senha por achar que ela vazou precisa saber que ela de fato mudou.
        if (passwordHasher.matches(command.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha precisa ser diferente da atual.");
        }

        user.changePassword(passwordHasher.hash(command.newPassword()));
        userRepository.save(user);
    }

    @Override
    public User execute(RegisterCommand command) {
        Email email = new Email(command.email());
        validatePassword(command.password());

        // O UNIQUE no banco é a garantia final; esta checagem existe para dar uma mensagem boa.
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Já existe uma conta com este e-mail.");
        }

        String hash = passwordHasher.hash(command.password());
        return provision(User.withPassword(email, command.name(), hash));
    }

    @Override
    public User execute(LoginCommand command) {
        Optional<User> found = parseEmail(command.email()).flatMap(userRepository::findByEmail);

        // A comparação roda SEMPRE, mesmo sem usuário: o adapter gasta o mesmo tempo contra um
        // hash descartável. Se saíssemos mais cedo, o tempo de resposta diria quais e-mails existem.
        String hash = found.map(User::getPasswordHash).orElse(null);
        boolean passwordMatches = passwordHasher.matches(command.password(), hash);

        if (found.isEmpty() || !found.get().hasPassword() || !passwordMatches) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        return found.get();
    }

    @Override
    public User execute(GoogleLoginCommand command) {
        Objects.requireNonNull(command.googleId(), "O id do Google é obrigatório.");

        // 1. Já conhecemos esta conta Google: é o caminho de todo login recorrente.
        Optional<User> byGoogleId = userRepository.findByGoogleId(command.googleId());
        if (byGoogleId.isPresent()) {
            return byGoogleId.get();
        }

        Email email = new Email(command.email());
        Optional<User> byEmail = userRepository.findByEmail(email);

        // 2. Já existe conta com este e-mail: vincula, para a pessoa não acabar com duas contas.
        if (byEmail.isPresent()) {
            if (!command.emailVerified()) {
                throw new AuthenticationException(
                        "O Google não confirmou este e-mail, então ele não pode ser vinculado a uma conta existente.");
            }
            User user = byEmail.get();
            user.linkGoogle(command.googleId());
            return userRepository.save(user);
        }

        // 3. Primeiro contato: cria a conta sem senha.
        return provision(User.withGoogle(email, command.name(), command.googleId()));
    }

    /**
     * Grava a conta nova e entrega as categorias padrão junto.
     *
     * <p>Existe como método privado compartilhado justamente porque há <strong>duas</strong>
     * portas de entrada que criam conta: o cadastro por senha e o primeiro login com Google.
     * Se o provisionamento vivesse no controller, ele teria que ser chamado nos dois lugares —
     * e o caminho do Google nem consegue distinguir "acabei de criar" de "já existia".
     *
     * <p>Como tudo acontece na mesma transação, não existe estado intermediário: ou nasce a
     * conta com suas categorias, ou não nasce nada.
     */
    private User provision(User novoUsuario) {
        User salvo = userRepository.save(novoUsuario);
        categoryRepository.saveAll(Category.defaultsFor(salvo.getId()));
        return salvo;
    }

    @Override
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + userId));
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("A senha deve ter no mínimo " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
    }

    /** E-mail malformado no login é só mais uma credencial inválida — não merece mensagem própria. */
    private Optional<Email> parseEmail(String raw) {
        try {
            return Optional.of(new Email(raw));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
