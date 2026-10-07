-- The catalog service's schema, consolidated from the monolith's migrations at the moment it left
-- (step 5b): init_schema, reference_categories, timestamps_with_time_zone, checkout_saga_preparation
-- and checkout_saga — only the catalog's and the inventory's tables. Same PostgreSQL/H2 common subset.

-- --- catalog ---------------------------------------------------------------

create table categories (
    id      uuid         not null,
    name    varchar(255) not null unique,
    active  boolean      not null,
    primary key (id)
);

create table books (
    id               uuid                        not null,
    title            varchar(255)                not null,
    isbn             varchar(255)                not null unique,
    publisher        varchar(255),
    publication_year integer,
    edition          varchar(255),
    synopsis         text,
    price            numeric(10, 2)              not null,
    stock_quantity   integer                     not null,
    cover_image_url  varchar(255),
    category_id      uuid                        not null,
    avg_rating       double precision            not null,
    active           boolean                     not null,
    created_at       timestamp(6) with time zone not null,
    updated_at       timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_books_category foreign key (category_id) references categories (id)
);
create index idx_books_category on books (category_id);

create table book_authors (
    book_id uuid         not null,
    author  varchar(255) not null,
    constraint fk_book_authors_book foreign key (book_id) references books (id)
);
create index idx_book_authors_book on book_authors (book_id);

-- Reference data. The UUIDs are referenced literally by DevDataLoader (catalog's
-- infrastructure/bootstrap): changing them breaks the dev book seed.
insert into categories (id, name, active) values
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Ficção Científica',     true),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Romance',               true),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Tecnologia',            true),
    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'História',              true),
    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'Negócios',              true),
    ('f6a7b8c9-d0e1-2345-fabc-456789012345', 'Autoajuda',             true),
    ('a7b8c9d0-e1f2-3456-abcd-567890123456', 'Literatura Brasileira', true),
    ('b8c9d0e1-f2a3-4567-bcde-678901234567', 'Infantil',              true);

-- --- stock reservations (checkout saga) -----------------------------------------

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

-- --- inventory -----------------------------------------------------------------

create table inventory_entries (
    id                uuid                        not null,
    book_id           uuid                        not null,
    delta             integer                     not null,
    previous_quantity integer                     not null,
    new_quantity      integer                     not null,
    reason            varchar(255),
    adjusted_by       uuid,
    adjusted_at       timestamp(6) with time zone not null,
    primary key (id)
);
create index idx_inventory_entries_book on inventory_entries (book_id);

-- --- messaging: the catalog's own outbox and inbox --------------------------------

create table catalog_outbox (
    id           uuid                        not null,
    aggregate_id uuid                        not null,
    event_type   varchar(100)                not null,
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
