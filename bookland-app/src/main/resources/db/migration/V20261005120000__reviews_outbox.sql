-- Transactional outbox of the reviews module. An event is written here in the same transaction as
-- the review that caused it, so the two are stored together or not at all; a relay then publishes
-- the pending rows to Kafka and stamps published_at. A broker that is down delays the event instead
-- of losing it.
--
-- Named after the module, not "outbox": while the modules share one database each needs its own.
create table reviews_outbox (
    id           uuid                        not null,  -- the event's eventId
    aggregate_id uuid                        not null,  -- becomes the Kafka message key (the book id)
    event_type   varchar(100)                not null,  -- the relay picks the topic from it
    payload      text                        not null,  -- the JSON exactly as it will be sent
    created_at   timestamp(6) with time zone not null,
    published_at timestamp(6) with time zone,           -- null while pending
    primary key (id)
);

-- What the relay asks for on every round: the pending rows, oldest first. A partial index
-- (where published_at is null) would be smaller on PostgreSQL, but H2 does not accept one.
create index idx_reviews_outbox_pending on reviews_outbox (published_at, created_at);
