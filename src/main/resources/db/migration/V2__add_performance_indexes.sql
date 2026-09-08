-- =====================================================================
-- V2 — Indices para as consultas que a aplicacao realmente faz.
--
-- Sem eles, toda listagem varre a tabela inteira (sequential scan) e depois
-- descarta as linhas de outros usuarios. Funciona com 10 registros; degrada
-- linearmente a partir dai.
-- =====================================================================

-- Atende as DUAS consultas de despesa:
--   findByUserId(userId)
--   findByUserIdAndDueDateBetween(userId, inicio, fim)
--
-- Um indice composto tambem serve consultas que filtram apenas pela sua
-- PRIMEIRA coluna. Por isso NAO criamos um indice separado so de user_id:
-- seria redundante, e todo indice extra custa espaco e torna INSERT/UPDATE
-- mais lentos.
CREATE INDEX ix_expenses_user_due_date ON tb_expenses (user_id, due_date);

-- Metas so sao consultadas por dono.
CREATE INDEX ix_goals_user_id ON tb_financial_goals (user_id);
