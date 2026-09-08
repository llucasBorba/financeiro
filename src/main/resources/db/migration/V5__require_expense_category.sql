-- =====================================================================
-- V5 — Categoria passa a ser obrigatoria na despesa.
--
-- Sem isto, uma despesa sem categoria some da quebra do resumo mensal:
-- o valor entra no total, mas nao aparece em nenhuma fatia. O buraco fica
-- invisivel. Como toda conta ja nasce com "Outros", exigir a categoria nao
-- pesa — e transforma um dado ausente num dado explicito.
-- =====================================================================

-- Backfill das despesas antigas. Preferencia por "Outros"; se o usuario a
-- renomeou ou apagou, cai na primeira categoria dele em ordem alfabetica.
--
-- Se algum usuario tiver despesa sem categoria E nenhuma categoria cadastrada,
-- o COALESCE devolve NULL e o ALTER abaixo FALHA — de proposito. Migracao que
-- falha alto e melhor do que migracao que inventa dado ou apaga linha.
UPDATE tb_expenses e
   SET category_id = COALESCE(
           (SELECT c.id FROM tb_categories c
             WHERE c.user_id = e.user_id AND c.name = 'Outros' LIMIT 1),
           (SELECT c.id FROM tb_categories c
             WHERE c.user_id = e.user_id ORDER BY c.name LIMIT 1))
 WHERE e.category_id IS NULL;

ALTER TABLE tb_expenses ALTER COLUMN category_id SET NOT NULL;
