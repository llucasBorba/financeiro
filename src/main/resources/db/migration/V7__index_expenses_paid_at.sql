-- =====================================================================
-- V7 — Indice para o resumo mensal.
--
-- O resumo apura o que SAIU pelo regime de caixa: despesas cujo PAGAMENTO
-- caiu dentro do mes, independente de quando venceram. Ate aqui so existia
-- indice por (user_id, due_date), que nao atende essa consulta.
--
-- NOTA: o ideal no Postgres seria um indice PARCIAL — "WHERE paid_at IS NOT
-- NULL" — porque despesa pendente tem paid_at nulo e nunca sera procurada por
-- essa coluna. Isso deixaria o indice menor. Mas indice parcial e sintaxe
-- especifica do Postgres: o H2 dos testes recusa o comando.
--
-- Escolha: SQL portavel, que roda igual nos dois bancos. O ganho do indice
-- parcial e marginal nesta escala; ja a garantia de que as migracoes sao
-- exercitadas pela suite de testes vale muito. Se um dia adotarmos
-- Testcontainers (Postgres de verdade nos testes), da para revisitar.
-- =====================================================================

CREATE INDEX ix_expenses_user_paid_at ON tb_expenses (user_id, paid_at);
