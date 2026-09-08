# Financeiro

API de controle de gastos pessoais: despesas, receitas, categorias, metas,
despesas recorrentes e fechamento mensal.

Spring Boot 4 · Java 21 · PostgreSQL · arquitetura hexagonal

---

## Como subir

**Pré-requisitos:** JDK 21+ e Docker (para o Postgres).

```bash
./mvnw spring-boot:run
```

Só isso. O `spring-boot-docker-compose` lê o `compose.yaml`, sobe o Postgres e
injeta a conexão; o Flyway cria o esquema na primeira execução.

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

```bash
./mvnw test        # 207 testes, banco H2 em memória, sem Docker
./mvnw clean test  # depois de mudar assinatura de record ou construtor — ver "Armadilhas"
```

## Primeiros passos na API

Tudo sob `/api/**` exige um token. O fluxo é:

```bash
# 1. cria a conta e já recebe o token
curl -X POST localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"voce@exemplo.com","password":"uma-senha-de-8-ou-mais","name":"Você"}'

# 2. a conta nasce com 8 categorias — pegue o id de uma
curl localhost:8080/api/categories -H "Authorization: Bearer $TOKEN"

# 3. lance uma despesa já paga (compra à vista)
curl -X POST localhost:8080/api/expenses \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"categoryId":"<id>","amount":19.01,"description":"Bob'\''s",
       "dueDate":"2026-09-08","paidAt":"2026-09-08T12:30:00"}'

# 4. veja o fechamento do mês
curl "localhost:8080/api/summary/monthly?month=2026-09" -H "Authorization: Bearer $TOKEN"
```

No Swagger, clique em **Authorize** e cole o `accessToken` — as rotas passam a funcionar.

## Endpoints

| Recurso | |
|---|---|
| `/auth` | `register` · `login` · `google` · `me` |
| `/api/expenses` | `POST` `GET` `PUT` `DELETE` · `PATCH`/`DELETE` `/{id}/payment` |
| `/api/incomes` | `POST` `GET` `PUT` `DELETE` |
| `/api/categories` | `POST` `GET` `PUT` `DELETE` · `PATCH /{id}/active` |
| `/api/goals` | `POST` `GET` `PUT` `DELETE` · `POST /{id}/deposits` |
| `/api/recurring-expenses` | `POST` `GET` `PUT` `DELETE` · `POST /{id}/generate` |
| `/api/summary/monthly` | `?month=2026-09` |

## Configuração

| Variável | Padrão | |
|---|---|---|
| `JWT_SECRET` | **nenhum** | Mínimo 32 bytes. **Sem ela a aplicação não sobe** — segredo com padrão é segredo público. Em desenvolvimento vem de `application-dev.properties`. |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres local | |
| `SPRING_PROFILES_ACTIVE` | `dev` | Em produção, `prod`. |
| `JWT_EXPIRATION` | `PT1H` | Duração ISO-8601. |
| `JPA_DDL_AUTO` | `validate` | O esquema pertence ao Flyway. |
| `FLYWAY_BASELINE` | `false` | `true` só ao adotar Flyway num banco que já tem tabelas. |

## Arquitetura

```
core/            regras de negócio — ZERO framework
  model/           agregados e value objects
  ports/ingoing/   "o que você pode me pedir"    ← core implementa, infra chama
  ports/outgoing/  "o que eu preciso de você"    ← core chama, infra implementa
  service/         orquestração dos casos de uso

infrastructure/  tudo que é substituível
  database/        JPA, mappers, adapters
  config/security/ JWT, BCrypt, Google, Spring Security
  identity/        @CurrentUser — de onde vem o usuário da requisição
  transaction/     decorators com @Transactional
  web/             controllers e DTOs
  config/          UseCaseConfig — o único lugar que conhece os dois mundos
```

A regra: **`core` não importa nada de `infrastructure`.** Dá para verificar:

```bash
grep -r "org.springframework\|jakarta" src/main/java/br/com/gastos/financeiro/core/
# nenhum resultado
```

É por isso que os testes de domínio rodam em milissegundos: eles trocam o
Postgres por um `HashMap` sem tocar numa linha do núcleo.

## Banco de dados

O esquema pertence às migrações em `src/main/resources/db/migration/`. O
Hibernate roda com `ddl-auto=validate`: ele **confere** e recusa subir se código
e banco divergirem.

**Mudou uma `@Entity`?**

```
1. ./mvnw test                → "Schema validation: missing column [x]"
2. escreva db/migration/V11__descricao.sql
3. ./mvnw test                → verde
4. commite a entidade E a migração JUNTAS
```

O passo 4 não é detalhe: quem der `git pull` com um só dos dois fica com uma
aplicação que não sobe.

**Migração já aplicada é imutável.** O Flyway guarda um checksum; editar um
`.sql` que já rodou faz ele recusar a subida. Errou? Escreva a próxima versão.

## Armadilhas conhecidas

- **`./mvnw clean` depois de mudar a assinatura de um `record` ou construtor.** A
  compilação incremental reaproveita `.class` antigos e dá falso verde.
- **SQL portável nas migrações.** Os testes rodam em H2 e a produção em Postgres.
  Índice parcial (`CREATE INDEX ... WHERE`) é sintaxe só do Postgres e quebra a suíte.
- **Números de migração só crescem.** Não "reserve" um número para depois: o Flyway
  recusa uma versão menor que a já aplicada.

## Limitações conhecidas

- **Sem verificação de e-mail.** Quem cadastrar um e-mail antes do dono pode
  herdar a conta quando ele entrar pelo Google. O campo `emailVerified` já existe
  esperando o fluxo de confirmação.
- **Sem refresh token.** Token de 1 hora; depois, relogar. Token vazado vale até
  expirar — não há revogação.
- **Sem paginação.** As listagens devolvem tudo.
- **Sem CORS.** Precisa ser configurado quando houver um front.

## Licença

Projeto de estudo.
