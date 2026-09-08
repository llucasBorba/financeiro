package br.com.gastos.financeiro.infrastructure.web;

import br.com.gastos.financeiro.core.exception.AuthenticationException;
import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz as exceções do domínio em respostas HTTP no formato RFC 7807 (ProblemDetail),
 * evitando que qualquer erro de negócio vire um 500 genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    // TODO: quando a autenticacao entrar, a violacao de propriedade ("Acesso negado")
    // deve virar 403/404 em vez de 400.
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Regra de negócio violada", ex.getMessage());
    }

    /** Estado inválido para a operação — ex.: pagar uma despesa que já está paga. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, "Operação não permitida no estado atual", ex.getMessage());
    }

    /** Argumento inválido vindo do domínio — ex.: moedas diferentes, saldo insuficiente. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    /**
     * Duas requisições tentaram alterar o mesmo registro ao mesmo tempo e a coluna
     * {@code version} barrou a segunda.
     *
     * <p>Não é erro de servidor (500): o pedido era válido, apenas chegou com uma leitura
     * desatualizada. 409 comunica exatamente isso e o cliente pode simplesmente repetir a
     * chamada. Uma evolução possível é a própria aplicação tentar de novo automaticamente
     * (ex.: Spring Retry), já que aporte e baixa de despesa são operações seguras de repetir.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Conflito de concorrência ao gravar o registro", ex);
        return problem(HttpStatus.CONFLICT, "Conflito de concorrência",
                "O registro foi alterado por outra operação. Consulte os dados atualizados e tente de novo.");
    }

    /**
     * Credencial ausente, inválida ou expirada.
     *
     * <p>Cobre tanto a exceção do domínio quanto a do Spring Security (token malformado, sem
     * assinatura válida, vencido). A mensagem devolvida é sempre genérica: detalhar qual parte
     * do token falhou só ajudaria quem está tentando forjar um.
     */
    @ExceptionHandler({AuthenticationException.class,
            org.springframework.security.core.AuthenticationException.class})
    public ProblemDetail handleAuthentication(Exception ex) {
        log.debug("Falha de autenticação", ex);
        return problem(HttpStatus.UNAUTHORIZED, "Não autenticado",
                ex instanceof AuthenticationException domainEx
                        ? domainEx.getMessage()
                        : "Credenciais ausentes ou inválidas.");
    }

    /** Autenticado, mas sem permissão para a operação. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para executar esta operação.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos da requisição são inválidos.");
        body.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
