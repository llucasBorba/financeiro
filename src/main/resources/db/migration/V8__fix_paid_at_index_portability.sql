-- =====================================================================
-- V8 — Corrige o indice da V7 para a forma portavel.
--
-- HISTORIA: a V7 nasceu com indice PARCIAL ("WHERE paid_at IS NOT NULL").
-- O Postgres aceitou e aplicou. O H2 dos testes recusou a sintaxe, entao o
-- arquivo foi reescrito sem o WHERE — e o checksum deixou de bater com o que
-- ja estava gravado no banco de desenvolvimento.
--
-- Editar a V7 foi um erro: migracao aplicada e IMUTAVEL. O jeito certo e o
-- que esta aqui — uma migracao NOVA que corrige o estado. Assim todo ambiente
-- percorre a mesma sequencia: V7 cria (de um jeito), V8 ajusta.
--
-- DROP IF EXISTS porque o indice pode estar em duas formas diferentes
-- dependendo de quando o ambiente foi criado.
-- =====================================================================

DROP INDEX IF EXISTS ix_expenses_user_paid_at;

CREATE INDEX ix_expenses_user_paid_at ON tb_expenses (user_id, paid_at);
