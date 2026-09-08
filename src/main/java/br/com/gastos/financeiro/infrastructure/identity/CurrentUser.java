package br.com.gastos.financeiro.infrastructure.identity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um parâmetro de controller que deve receber o id do usuário autenticado.
 *
 * <p>O controller declara apenas <em>que precisa</em> do usuário atual; quem decide
 * <em>de onde ele vem</em> é o {@link CurrentUserProvider}. Enquanto a autenticação real
 * não existe, o provider de desenvolvimento lê um header. Quando o JWT entrar, só o
 * provider muda — nenhum controller, DTO ou caso de uso precisa ser tocado.
 *
 * <pre>{@code
 * @GetMapping
 * public ResponseEntity<List<GoalResponse>> list(@CurrentUser UUID userId) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
