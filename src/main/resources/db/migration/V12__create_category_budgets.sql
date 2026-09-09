-- Limite mensal de gasto por categoria.
--
-- O limite é fixo: vale para todos os meses. Por isso não há coluna de mês aqui — o mês entra
-- só na CONSULTA, que confronta este limite com os lançamentos do período. Guardar uma linha
-- por mês obrigaria o usuário a redigitar o orçamento todo mês, e criaria o problema clássico
-- do dado derivado: linhas de meses passados que envelhecem quando uma despesa antiga é paga.

CREATE TABLE tb_category_budgets (
    id            uuid          NOT NULL,
    user_id       uuid          NOT NULL,
    category_id   uuid          NOT NULL,
    monthly_limit numeric(15,2) NOT NULL,
    currency      varchar(3)    NOT NULL,
    version       bigint,

    CONSTRAINT pk_category_budgets PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user     FOREIGN KEY (user_id)     REFERENCES tb_users (id),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES tb_categories (id),

    -- No máximo um limite por categoria. É o que torna PUT /api/budgets/{categoryId}
    -- idempotente e o que dá à categoria o papel de identidade natural do recurso.
    -- O serviço já faz busca-e-atualiza; este índice é a rede contra duas requisições
    -- simultâneas passando pela busca antes de qualquer uma gravar.
    CONSTRAINT ux_budgets_user_category UNIQUE (user_id, category_id),

    -- Limite zero seria "não posso gastar nada aqui", que na prática é arquivar a categoria.
    CONSTRAINT ck_budgets_positive CHECK (monthly_limit > 0)
);

-- Toda leitura de orçamento filtra por dono, igual a todas as outras listagens.
-- O índice único acima já começa por user_id e atende essa consulta, então não há
-- índice adicional aqui de propósito: índice redundante custa escrita e não paga nada.
