-- =============================================================================
-- Bookland — preparação da saga do checkout (passo 4a)
--
-- Três módulos, cada um com o que é seu; nenhuma FK atravessa módulos.
-- =============================================================================


-- --- catalog: reserva de estoque por pedido -----------------------------------
-- Uma reserva por pedido (order_id é a chave), o que torna reservar e liberar
-- idempotentes: um pedido repetido encontra a linha e responde a partir dela.
-- order_id NÃO referencia orders: é de outro módulo. status: RESERVED, RELEASED
-- ou FAILED (a reserva que não pôde ser feita também fica registrada).

create table stock_reservations (
    order_id   uuid                        not null,
    status     varchar(20)                 not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    primary key (order_id)
);

create table stock_reservation_items (
    order_id uuid    not null,
    book_id  uuid    not null,
    quantity integer not null,
    primary key (order_id, book_id),
    foreign key (order_id) references stock_reservations (order_id)
);


-- --- orders: o motivo de um pedido não ter sido concluído ----------------------
-- Os livros sem estoque (REJECTED) ou o motivo da recusa (PAYMENT_FAILED).
-- Os status novos (PENDING, REJECTED) não precisam de migration: a coluna status
-- é varchar sem CHECK.

alter table orders add column status_reason varchar(500);


-- --- payments: um pagamento por pedido -----------------------------------------
-- É o que torna a cobrança idempotente: um pedido de cobrança repetido encontra o
-- pagamento existente e responde com o mesmo resultado, sem cobrar de novo.

drop index idx_payments_order;
create unique index uk_payments_order on payments (order_id);

-- O motivo da recusa, guardado para que um pedido de cobrança repetido receba a
-- mesma resposta do primeiro sem perguntar de novo ao gateway.
alter table payments add column decline_reason varchar(255);
