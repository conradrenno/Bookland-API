-- The notification service's own database. For now it holds only the inbox: one row per order
-- event already turned into an email task, written in the transaction that queued the task, so a
-- redelivered event is recognised and not emailed twice.
create table notification_inbox (
    message_id   uuid                        not null,
    processed_at timestamp(6) with time zone not null,
    primary key (message_id)
);
