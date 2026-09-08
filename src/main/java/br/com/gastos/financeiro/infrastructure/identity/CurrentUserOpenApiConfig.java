package br.com.gastos.financeiro.infrastructure.identity;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensina o springdoc a lidar com {@link CurrentUser} e a documentar o esquema de autenticação.
 *
 * <p>Sem o {@code addAnnotationsToIgnore}, o springdoc vê um parâmetro {@code UUID} sem nenhuma
 * anotação conhecida e assume que é um {@code @RequestParam} implícito — publicando um query
 * param {@code userId} obrigatório que não existe, e travando o Swagger UI.
 */
@Configuration
public class CurrentUserOpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    static {
        // Parâmetros @CurrentUser não fazem parte do contrato: quem os preenche é o servidor.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    /**
     * Declara o esquema Bearer para o Swagger mostrar o botão <em>Authorize</em>.
     *
     * <p>O fluxo no Swagger passa a ser: chamar {@code /auth/register} ou {@code /auth/login},
     * copiar o {@code accessToken} da resposta, colar no Authorize — e todas as rotas
     * {@code /api/**} passam a funcionar.
     */
    @Bean
    public OpenAPI financeiroOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Financeiro API")
                        .version("v1")
                        .description("Controle de gastos e metas financeiras."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido por /auth/login, /auth/register ou /auth/google.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
