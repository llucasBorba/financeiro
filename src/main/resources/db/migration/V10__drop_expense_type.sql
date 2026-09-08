-- =====================================================================
-- V10 — Remove a coluna 'type' das despesas.
--
-- FIXED/VARIABLE nunca alterou comportamento nenhum: era um rotulo que o
-- usuario preenchia e o sistema devolvia. Pior, ele competia com a categoria
-- na cabeca de quem monta a requisicao ("Alimentacao e um type?").
--
-- Agora existe um fato verificavel no lugar da declaracao de intencao:
-- recurring_expense_id preenchido = a despesa nasceu de uma recorrencia.
--
-- Nada e migrado porque nada dependia do valor: nenhuma consulta, regra ou
-- relatorio usava a coluna.
-- =====================================================================

ALTER TABLE tb_expenses DROP COLUMN type;
