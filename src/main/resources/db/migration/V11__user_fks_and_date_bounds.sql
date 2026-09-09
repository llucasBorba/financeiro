-- Duas lacunas que compartilham a mesma causa: o banco estava aceitando dados que
-- o domínio nunca deveria ter deixado passar, e não tinha como recusá-los sozinho.

-- ---------------------------------------------------------------------------
-- 1. Chaves estrangeiras para tb_users
-- ---------------------------------------------------------------------------
-- Todas as cinco tabelas guardam user_id NOT NULL, mas nenhuma tinha FK. Hoje isso é
-- inofensivo porque nada apaga usuário; no dia em que "excluir minha conta" existir, um
-- DELETE deixaria despesas, receitas, categorias e metas apontando para um dono que não
-- existe mais — invisíveis na API (toda consulta filtra por dono) e impossíveis de limpar
-- sem saber de quem eram.
--
-- Sem ON DELETE CASCADE de propósito: apagar um usuário passa a FALHAR enquanto ele tiver
-- dados. Isso é o que queremos agora — nenhum caminho do sistema deve apagar usuário por
-- acidente. Quando a exclusão de conta for implementada, ela será uma operação explícita e
-- ordenada dentro de uma transação, não um efeito colateral de um DELETE solto.
--
-- É o mesmo ON DELETE das FKs de categoria que já existiam (NO ACTION), por coerência.

ALTER TABLE tb_expenses
    ADD CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES tb_users (id);

ALTER TABLE tb_incomes
    ADD CONSTRAINT fk_incomes_user FOREIGN KEY (user_id) REFERENCES tb_users (id);

ALTER TABLE tb_categories
    ADD CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES tb_users (id);

ALTER TABLE tb_financial_goals
    ADD CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES tb_users (id);

ALTER TABLE tb_recurring_expenses
    ADD CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES tb_users (id);

-- ---------------------------------------------------------------------------
-- 2. Faixa de datas aceita
-- ---------------------------------------------------------------------------
-- LocalDate vai até o ano 999999999 e nada estreitava isso. Pior: o driver pgjdbc traduz
-- LocalDate.MAX para o valor especial 'infinity' do Postgres, então a linha era gravada com
-- 201 Created e depois sumia de todo filtro de mês (nada é <= infinity). Uma despesa que
-- existe, foi aceita, e nenhum relatório encontra.
--
-- Quem decide a faixa é DateBounds, no core. Estes CHECKs são a segunda barreira — a mesma
-- relação que já temos entre as migrações e ddl-auto=validate: o código manda, o banco audita.
-- Também garantem que nenhuma linha fora da faixa possa existir, o que é o que torna seguro
-- validar na reconstituição das entidades.

ALTER TABLE tb_expenses
    ADD CONSTRAINT ck_expenses_due_date
    CHECK (due_date BETWEEN DATE '1900-01-01' AND DATE '2200-12-31');

-- paid_at é timestamp: o limite superior é o INÍCIO de 2201, para que qualquer horário do
-- dia 2200-12-31 continue válido.
ALTER TABLE tb_expenses
    ADD CONSTRAINT ck_expenses_paid_at
    CHECK (paid_at IS NULL
           OR (paid_at >= TIMESTAMP '1900-01-01 00:00:00'
               AND paid_at < TIMESTAMP '2201-01-01 00:00:00'));

ALTER TABLE tb_incomes
    ADD CONSTRAINT ck_incomes_received_at
    CHECK (received_at BETWEEN DATE '1900-01-01' AND DATE '2200-12-31');

-- target_date é opcional na meta.
ALTER TABLE tb_financial_goals
    ADD CONSTRAINT ck_goals_target_date
    CHECK (target_date IS NULL
           OR target_date BETWEEN DATE '1900-01-01' AND DATE '2200-12-31');

-- start_month/end_month também são colunas date (sempre o dia 1 do mês).
ALTER TABLE tb_recurring_expenses
    ADD CONSTRAINT ck_recurring_start_month
    CHECK (start_month BETWEEN DATE '1900-01-01' AND DATE '2200-12-31');

ALTER TABLE tb_recurring_expenses
    ADD CONSTRAINT ck_recurring_end_month
    CHECK (end_month IS NULL
           OR end_month BETWEEN DATE '1900-01-01' AND DATE '2200-12-31');
