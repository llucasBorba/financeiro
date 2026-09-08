-- =====================================================================
-- V9 — Despesas recorrentes.
--
-- O modelo guarda a REGRA ("aluguel, R$1500, todo dia 10"); as ocorrencias
-- sao despesas de verdade em tb_expenses, geradas a partir dele.
--
-- A alternativa seria calcular as ocorrencias na hora da consulta, sem gravar
-- nada. Ficou de fora porque cada ocorrencia aqui tem vida propria: e paga
-- numa data, pode ter valor corrigido, pode ser apagada. Isso exige uma linha
-- de verdade — e com linhas reais, listagem, resumo e pagamento funcionam
-- sem nenhuma mudanca.
-- =====================================================================

CREATE TABLE tb_recurring_expenses (
    id           uuid          NOT NULL,
    user_id      uuid          NOT NULL,
    category_id  uuid          NOT NULL,
    amount       numeric(15,2) NOT NULL,
    currency     varchar(3)    NOT NULL,
    description  varchar(255)  NOT NULL,
    -- 1..31. Meses mais curtos grudam no ultimo dia (31 em fevereiro = 28/29).
    day_of_month integer       NOT NULL,
    start_month  date          NOT NULL,   -- sempre o dia 1 do mes inicial
    end_month    date,                     -- nulo = sem fim previsto
    active       boolean       NOT NULL DEFAULT true,
    version      bigint,

    CONSTRAINT pk_recurring_expenses PRIMARY KEY (id),
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES tb_categories (id),
    CONSTRAINT ck_recurring_day CHECK (day_of_month BETWEEN 1 AND 31)
);

-- A origem de cada lancamento gerado. Nulo = despesa avulsa, digitada a mao.
--
-- ON DELETE SET NULL de proposito: apagar o modelo NAO deve apagar o historico
-- ja pago. A despesa perde o vinculo e vira um lancamento avulso — que e
-- exatamente o que ela e, depois que a recorrencia deixou de existir.
ALTER TABLE tb_expenses
    ADD COLUMN recurring_expense_id uuid;

ALTER TABLE tb_expenses
    ADD CONSTRAINT fk_expenses_recurring
    FOREIGN KEY (recurring_expense_id) REFERENCES tb_recurring_expenses (id)
    ON DELETE SET NULL;

CREATE INDEX ix_recurring_user ON tb_recurring_expenses (user_id);

-- Usado para: "quais ocorrencias este modelo ja gerou?" — na geracao (para nao
-- duplicar mes), na edicao e na exclusao em cascata.
CREATE INDEX ix_expenses_recurring ON tb_expenses (recurring_expense_id);
