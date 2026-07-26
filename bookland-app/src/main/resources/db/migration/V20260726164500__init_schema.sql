-- =============================================================================
-- Bookland — schema inicial
--
-- Baseline gerada a partir do mapeamento JPA das 14 entidades (Hibernate
-- schema-generation com PostgreSQLDialect) e curada: constraints nomeadas,
-- colunas reordenadas e índices acrescentados.
--
-- Esta V1 absorve dois DDLs que nunca chegaram a ser aplicados em nenhum
-- ambiente, por o banco de produção não existir até aqui:
--   * order_items.cover_image_url  (feat: coverImageUrl nos item payloads)
--   * normalização de ISBN para 13 dígitos (fix(catalog))
--
-- Chaves estrangeiras existem apenas dentro de um mesmo módulo. Colunas que
-- referenciam outro módulo (book_id em cart_items/reviews/wishlist_items/
-- inventory_entries, customer_id, order_id em payments, user_id em
-- refresh_tokens) são uuid sem FK, refletindo a ausência de relacionamento
-- JPA entre módulos. Elas são indexadas, mas não têm integridade referencial.
-- =============================================================================


-- --- catalog -----------------------------------------------------------------

create table categories (
    id      uuid         not null,
    name    varchar(255) not null unique,
    active  boolean      not null,
    primary key (id)
);

create table books (
    id               uuid           not null,
    title            varchar(255)   not null,
    isbn             varchar(255)   not null unique,
    publisher        varchar(255),
    publication_year integer,
    edition          varchar(255),
    synopsis         text,
    price            numeric(10, 2) not null,
    stock_quantity   integer        not null,
    cover_image_url  varchar(255),
    category_id      uuid           not null,
    avg_rating       double precision not null,
    active           boolean        not null,
    created_at       timestamp(6)   not null,
    updated_at       timestamp(6)   not null,
    primary key (id)
);

-- @ElementCollection de autores: sem chave primária, por definição do mapeamento
create table book_authors (
    book_id uuid         not null,
    author  varchar(255) not null
);


-- --- user --------------------------------------------------------------------

create table users (
    id            uuid         not null,
    name          varchar(255) not null,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(255) not null check (role in ('CUSTOMER', 'ADMIN')),
    active        boolean      not null,
    created_at    timestamp(6) not null,
    updated_at    timestamp(6) not null,
    primary key (id)
);


-- --- auth --------------------------------------------------------------------

create table refresh_tokens (
    id          uuid                     not null,
    user_id     uuid                     not null,
    email       varchar(255)             not null,
    role        varchar(255)             not null,
    token_value varchar(255)             not null unique,
    revoked     boolean                  not null,
    created_at  timestamp(6) with time zone not null,
    expires_at  timestamp(6) with time zone not null,
    primary key (id)
);


-- --- orders ------------------------------------------------------------------

create table carts (
    id          uuid         not null,
    customer_id uuid         not null unique,
    created_at  timestamp(6) not null,
    updated_at  timestamp(6) not null,
    primary key (id)
);

create table cart_items (
    id                     uuid           not null,
    cart_id                uuid           not null,
    book_id                uuid           not null,
    quantity               integer        not null,
    unit_price_at_addition numeric(10, 2) not null,
    primary key (id)
);

create table orders (
    id           uuid           not null,
    customer_id  uuid           not null,
    status       varchar(255)   not null,
    total_amount numeric(10, 2) not null,
    created_at   timestamp(6)   not null,
    updated_at   timestamp(6)   not null,
    primary key (id)
);

create table order_items (
    id              uuid           not null,
    order_id        uuid           not null,
    book_id         uuid           not null,
    title           varchar(255)   not null,
    cover_image_url varchar(255),
    quantity        integer        not null,
    unit_price      numeric(10, 2) not null,
    primary key (id)
);

create table order_status_transitions (
    id          uuid         not null,
    order_id    uuid         not null,
    from_status varchar(255) not null,
    to_status   varchar(255) not null,
    changed_by  uuid,
    changed_at  timestamp(6) not null,
    primary key (id)
);


-- --- payments ----------------------------------------------------------------

create table payments (
    id                     uuid           not null,
    order_id               uuid           not null,
    customer_id            uuid           not null,
    amount                 numeric(10, 2) not null,
    method                 varchar(255)   not null,
    status                 varchar(255)   not null,
    gateway_transaction_id varchar(255),
    created_at             timestamp(6)   not null,
    updated_at             timestamp(6)   not null,
    primary key (id)
);


-- --- reviews -----------------------------------------------------------------

create table reviews (
    id          uuid         not null,
    book_id     uuid         not null,
    customer_id uuid         not null,
    rating      integer      not null,
    comment     text,
    deleted     boolean      not null,
    created_at  timestamp(6) not null,
    primary key (id)
);


-- --- wishlist ----------------------------------------------------------------

create table wishlists (
    id          uuid not null,
    customer_id uuid not null unique,
    primary key (id)
);

create table wishlist_items (
    id          uuid         not null,
    wishlist_id uuid         not null,
    book_id     uuid         not null,
    added_at    timestamp(6) not null,
    primary key (id)
);


-- --- inventory ---------------------------------------------------------------

create table inventory_entries (
    id                uuid         not null,
    book_id           uuid         not null,
    delta             integer      not null,
    previous_quantity integer      not null,
    new_quantity      integer      not null,
    reason            varchar(255),
    adjusted_by       uuid,
    adjusted_at       timestamp(6) not null,
    primary key (id)
);


-- --- chaves estrangeiras (intra-módulo) --------------------------------------

alter table book_authors
    add constraint fk_book_authors_book
    foreign key (book_id) references books (id);

alter table books
    add constraint fk_books_category
    foreign key (category_id) references categories (id);

alter table cart_items
    add constraint fk_cart_items_cart
    foreign key (cart_id) references carts (id);

alter table order_items
    add constraint fk_order_items_order
    foreign key (order_id) references orders (id);

alter table order_status_transitions
    add constraint fk_order_status_transitions_order
    foreign key (order_id) references orders (id);

alter table wishlist_items
    add constraint fk_wishlist_items_wishlist
    foreign key (wishlist_id) references wishlists (id);


-- --- índices -----------------------------------------------------------------
-- O PostgreSQL não indexa colunas de FK automaticamente, e as colunas de
-- referência entre módulos não têm FK nenhuma — todas são criadas aqui.

create index idx_book_authors_book on book_authors (book_id);
create index idx_books_category on books (category_id);

create index idx_refresh_tokens_user on refresh_tokens (user_id);

create index idx_cart_items_cart on cart_items (cart_id);
create index idx_cart_items_book on cart_items (book_id);
create index idx_orders_customer on orders (customer_id);
create index idx_orders_status on orders (status);
create index idx_order_items_order on order_items (order_id);
create index idx_order_items_book on order_items (book_id);
create index idx_order_status_transitions_order on order_status_transitions (order_id);

create index idx_payments_order on payments (order_id);
create index idx_payments_customer on payments (customer_id);

create index idx_reviews_book on reviews (book_id);
create index idx_reviews_customer on reviews (customer_id);

create index idx_wishlist_items_wishlist on wishlist_items (wishlist_id);
create index idx_wishlist_items_book on wishlist_items (book_id);

create index idx_inventory_entries_book on inventory_entries (book_id);
