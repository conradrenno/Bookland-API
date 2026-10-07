-- =============================================================================
-- Bookland — a saga assíncrona do checkout (passo 4b)
--
-- Cada módulo que participa ganha o SEU outbox (o que publica) e o SEU inbox
-- (o que já consumiu). Nada é compartilhado entre módulos: no dia em que cada um
-- tiver o próprio banco, estas tabelas vão junto, sem divisão.
--
-- Outbox: mesmo desenho do reviews_outbox (V20261005120000). Uma linha por
-- mensagem, gravada na mesma transação que a mudança que ela anuncia; um relay
-- envia as pendentes ao Kafka e carimba published_at.
--
-- Inbox: uma linha por mensagem consumida, gravada na mesma transação que o
-- efeito. Uma mensagem entregue de novo encontra a linha e é ignorada.
-- =============================================================================


-- --- catalog ------------------------------------------------------------------

create table catalog_outbox (
    id           uuid                        not null,  -- o messageId da mensagem
    aggregate_id uuid                        not null,  -- vira a chave Kafka (o id do pedido)
    event_type   varchar(100)                not null,  -- o relay escolhe o tópico por ele
    payload      text                        not null,
    created_at   timestamp(6) with time zone not null,
    published_at timestamp(6) with time zone,
    primary key (id)
);
create index idx_catalog_outbox_pending on catalog_outbox (published_at, created_at);

create table catalog_inbox (
    message_id   uuid                        not null,
    processed_at timestamp(6) with time zone not null,
    primary key (message_id)
);


-- --- payments -----------------------------------------------------------------

create table payments_outbox (
    id           uuid                        not null,
    aggregate_id uuid                        not null,
    event_type   varchar(100)                not null,
    payload      text                        not null,
    created_at   timestamp(6) with time zone not null,
    published_at timestamp(6) with time zone,
    primary key (id)
);
create index idx_payments_outbox_pending on payments_outbox (published_at, created_at);

create table payments_inbox (
    message_id   uuid                        not null,
    processed_at timestamp(6) with time zone not null,
    primary key (message_id)
);


-- --- orders -------------------------------------------------------------------

create table orders_outbox (
    id           uuid                        not null,
    aggregate_id uuid                        not null,
    event_type   varchar(100)                not null,
    payload      text                        not null,
    created_at   timestamp(6) with time zone not null,
    published_at timestamp(6) with time zone,
    primary key (id)
);
create index idx_orders_outbox_pending on orders_outbox (published_at, created_at);

create table orders_inbox (
    message_id   uuid                        not null,
    processed_at timestamp(6) with time zone not null,
    primary key (message_id)
);

-- A cobrança sai só quando a reserva responde, numa transação posterior: o pedido
-- precisa lembrar como o cliente escolheu pagar. Nula nos pedidos anteriores.
alter table orders add column payment_method varchar(20);

-- A trava do checkout em andamento: o pedido que reivindicou este carrinho. Um
-- update condicional (... where checkout_order_id is null) é o que impede dois
-- checkouts simultâneos do mesmo cliente — um índice único parcial faria o mesmo,
-- mas o H2 não suporta, e as migrations ficam no subconjunto PostgreSQL/H2.
alter table carts add column checkout_order_id uuid;
