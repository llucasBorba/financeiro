-- =====================================================================
-- V3 — Categorias de despesa.
--
-- Ate aqui, tb_expenses.category_id era um uuid solto: aceitava qualquer
-- valor inventado, sem tabela do outro lado e sem validacao nenhuma.
-- Esta migracao cria a tabela e finalmente amarra a chave estrangeira.
-- =====================================================================

CREATE TABLE tb_categories (
    id      uuid        NOT NULL,
    user_id uuid        NOT NULL,
    name    varchar(60) NOT NULL,
    -- Arquivar em vez de excluir: some das listas de selecao, mas o
    -- historico de despesas continua exibindo a categoria normalmente.
    active  boolean     NOT NULL DEFAULT true,
    version bigint,

    CONSTRAINT pk_categories PRIMARY KEY (id),
    -- Nome unico POR USUARIO. Dois usuarios podem ter "Alimentacao" cada um.
    CONSTRAINT ux_categories_user_name UNIQUE (user_id, name)
);

-- LIMPEZA DE DADOS antes de criar a FK.
--
-- As despesas existentes tem category_id apontando para o nada (eram UUIDs
-- livres). Se a FK fosse criada direto, o ALTER falharia por violacao de
-- integridade. Zeramos porque sabemos que aqueles valores nunca significaram
-- nada — e essa é uma informacao que so quem conhece a historia do sistema tem.
-- Nenhuma ferramenta de diff automatico escreveria esta linha.
UPDATE tb_expenses SET category_id = NULL;

ALTER TABLE tb_expenses
    ADD CONSTRAINT fk_expenses_category
    FOREIGN KEY (category_id) REFERENCES tb_categories (id);

-- Toda listagem de categoria filtra por dono.
CREATE INDEX ix_categories_user_id ON tb_categories (user_id);

-- Serve a pergunta "esta categoria esta em uso?", feita antes de toda exclusao.
CREATE INDEX ix_expenses_category_id ON tb_expenses (category_id);
