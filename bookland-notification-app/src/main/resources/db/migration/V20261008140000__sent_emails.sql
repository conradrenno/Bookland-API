-- Every email sent, one row per order and kind. Checked before sending, so a task that reaches the
-- sender twice — the same event queued twice after a crash, a retry of a send that did go through —
-- becomes one email. Also the history of what was sent to whom.
create table sent_emails (
    email_key varchar(100)                not null,  -- <orderId>:<KIND>
    recipient varchar(255)                not null,
    subject   varchar(500)                not null,
    sent_at   timestamp(6) with time zone not null,
    primary key (email_key)
);
