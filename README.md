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
| `JWT_SECRET` | **nenhum** | Mínimo 32 bytes. **Fora do profile `dev` a aplicação recusa subir sem ela.** Em desenvolvimento, quando ausente, um segredo aleatório é gerado a cada inicialização — os tokens deixam de valer a cada reinício. Defina a variável se quiser que sobrevivam. |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres local | |
| `SPRING_PROFILES_ACTIVE` | `dev` | **Defina como `prod` no deploy.** O padrão `dev` existe por conveniência local; esquecê-lo em produção roda a aplicação em modo de desenvolvimento. |
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
- **`MODE=PostgreSQL` no H2 emula sintaxe, não a faixa dos tipos.** Esta é a versão
  perigosa da armadilha acima, porque falha ao contrário: o teste passa e a produção
  diverge. O `DATE` do H2 cobre toda a faixa de `LocalDate` (até o ano 999999999);
  o do Postgres para em 5874897. Uma data absurda passava verde na suíte inteira.
  Pior ainda, o driver pgjdbc traduz `LocalDate.MAX` para o `infinity` do Postgres,
  então nem a produção dava erro — gravava **201 Created** uma linha que nenhum filtro
  de mês encontra (nada é `<= infinity`). Por isso limites de faixa moram no domínio
  (`DateBounds`, `Money.MAX_AMOUNT`), que vale igual nos dois bancos, e não em
  anotação de coluna.
- **Toda coluna com limite precisa do limite espelhado no domínio.** As 13 colunas
  `varchar` tinham `@Size` correspondente; as 5 `numeric(15,2)` e as 6 de data não
  tinham nada. O ponto cego não foi aleatório: seguiu o tipo cujo limite estava
  escrito de forma óbvia no `CREATE TABLE`.
- **Números de migração só crescem.** Não "reserve" um número para depois: o Flyway
  recusa uma versão menor que a já aplicada.

## Segredos

**Nenhum segredo é versionado, e nenhum deve ser.** O `JWT_SECRET` vem do ambiente; em
desenvolvimento, sem ele, um valor aleatório é gerado a cada inicialização.

Já houve um deslize aqui: um segredo de assinatura literal ficou em
`application-dev.properties` e o GitGuardian sinalizou. Como o profile padrão é `dev`, um
deploy que esquecesse `SPRING_PROFILES_ACTIVE=prod` assinaria tokens com um valor público —
qualquer pessoa que lesse o repositório poderia forjar um token para qualquer usuário. O valor
foi retirado e não é mais usado em lugar nenhum; ele permanece no histórico do git, e é
inofensivo justamente por ter deixado de valer.

A senha do Postgres no `compose.yaml` é de um contêiner local que só escuta em `localhost` —
não é credencial de produção.

## Trava de tentativas de login

Cinco erros para a mesma combinação de **e-mail e origem** travam aquela combinação por 15
minutos, com `429` e cabeçalho `Retry-After`. Um acerto zera a contagem.

A chave é o **par**, e não o e-mail sozinho, e essa é a decisão que importa: contando só por
e-mail, qualquer pessoa que saiba o seu endereço tranca você para fora da sua própria conta
com cinco senhas erradas, repetindo a cada expiração. A proteção viraria arma — é por isso
que bloqueio puro por conta é desaconselhado pelo OWASP e pelo NIST SP 800-63B. Com o par,
quem ataca tranca apenas `(e-mail, IP-do-atacante)`, e o dono continua entrando do IP dele.

O contador sobe antes de qualquer consulta ao banco, então o `429` é idêntico para conta
existente e inexistente — senão ele próprio viraria um jeito de descobrir quais e-mails estão
cadastrados.

**Ao hospedar atrás de um proxy, defina `FORWARD_HEADERS_STRATEGY=framework`.** Sem isso,
`getRemoteAddr()` devolve o IP do proxy para todo mundo, todos os usuários caem na mesma
origem, e a chave degenera de volta para só o e-mail. Só ative com um proxy confiável na
frente: sem proxy, o cliente forja o `X-Forwarded-For` e escapa da trava.

Duas coisas que esta versão não cobre, de propósito. O estado é em memória: reinicia zerado
num deploy (um atacante não ganha nada esperando) e não é compartilhado entre instâncias.
E um atacante mandando e-mails aleatórios de um IP só gera uma chave nova a cada tentativa,
então o contador nunca dispara e o bcrypt roda a cada requisição (~76 ms; ~13/s saturam um
núcleo). Cobrir isso exigiria um segundo contador por IP — foi deixado de fora por ser um
ataque que exige alguém mirando o sistema de propósito, e é um contador a mais na mesma
classe se um dia aparecer no log.

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
