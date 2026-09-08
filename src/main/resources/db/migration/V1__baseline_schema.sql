-- =====================================================================
-- V1 — Esquema de partida.
--
-- Descreve exatamente as tabelas que o Hibernate vinha criando sozinho
-- com ddl-auto=update. A partir daqui o Hibernate deixa de criar e passa
-- a apenas CONFERIR (ddl-auto=validate): se o codigo e o banco divergirem,
-- a aplicacao nao sobe.
--
-- Uma migracao ja aplicada e IMUTAVEL. O Flyway guarda um checksum de cada
-- arquivo; editar este arquivo depois de ele ter rodado faz o Flyway recusar
-- a subida. Precisou corrigir algo? Escreva a proxima versao.
-- =====================================================================

CREATE TABLE tb_users (
    id             uuid         NOT NULL,
    email          varchar(254) NOT NULL,
    name           varchar(255),
    -- Nulo para contas que so entram pelo Google.
    password_hash  varchar(100),
    -- Nulo para contas que so usam senha. UNIQUE aceita varios nulos.
    google_id      varchar(64),
    email_verified boolean      NOT NULL,
    created_at     timestamp(6) NOT NULL,
    -- Bloqueio otimista: o Hibernate incrementa a cada UPDATE.
    version        bigint,

    CONSTRAINT pk_users        PRIMARY KEY (id),
    CONSTRAINT ux_users_email  UNIQUE (email),
    CONSTRAINT ux_users_google UNIQUE (google_id)
);

CREATE TABLE tb_expenses (
    id          uuid          NOT NULL,
    user_id     uuid          NOT NULL,
    category_id uuid,
    amount      numeric(15,2) NOT NULL,
    currency    varchar(3)    NOT NULL,
    description varchar(255),
    due_date    date          NOT NULL,
    paid_at     timestamp(6),
    type        varchar(20)   NOT NULL,
    status      varchar(20)   NOT NULL,
    version     bigint,

    CONSTRAINT pk_expenses PRIMARY KEY (id)
);

CREATE TABLE tb_financial_goals (
    id             uuid          NOT NULL,
    user_id        uuid          NOT NULL,
    title          varchar(120)  NOT NULL,
    target_amount  numeric(15,2) NOT NULL,
    current_amount numeric(15,2) NOT NULL,
    currency       varchar(3)    NOT NULL,
    target_date    date,
    version        bigint,

    CONSTRAINT pk_financial_goals PRIMARY KEY (id)
);
