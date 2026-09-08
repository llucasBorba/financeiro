-- =====================================================================
-- V6 — Receitas.
--
-- Contrapartida da despesa: o dinheiro que entra. Repare no que NAO existe aqui:
--
--   * sem status / paidAt   — receita nao tem ciclo "a receber -> recebido".
--                             Voce registra o que ENTROU. Previsao de entrada e
--                             outro problema, aditivo, para quando fizer falta.
--
--   * sem category_id       — com 2 ou 3 lancamentos por mes, a lista JA e o
--                             relatorio; nao ha o que agregar. Categoria existe
--                             para quebrar 60 despesas, nao 3 receitas.
--
-- Em compensacao, description e OBRIGATORIA: sem categoria, ela e a unica
-- identidade do lancamento. "R$ 300 em 18/09" nao diz nada; "Pix do pai" diz tudo.
-- =====================================================================

CREATE TABLE tb_incomes (
    id          uuid          NOT NULL,
    user_id     uuid          NOT NULL,
    amount      numeric(15,2) NOT NULL,
    currency    varchar(3)    NOT NULL,
    description varchar(255)  NOT NULL,
    received_at date          NOT NULL,
    version     bigint,

    CONSTRAINT pk_incomes PRIMARY KEY (id)
);

-- Mesmo raciocinio do indice de despesas: composto com user_id na frente atende
-- tanto "minhas receitas" quanto "minhas receitas do periodo". Ja pensado para o
-- resumo mensal, que vai varrer as duas tabelas por (user_id, intervalo).
CREATE INDEX ix_incomes_user_received_at ON tb_incomes (user_id, received_at);
