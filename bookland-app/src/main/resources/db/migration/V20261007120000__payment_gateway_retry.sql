-- Payments are recorded before the gateway is called, and retried until it answers.
-- attempts / last_error: failed calls for the operation now pending, and why the last one failed
-- (or why the gateway refused a refund). next_attempt_at: when the gateway worker may call again;
-- null once nothing is pending. Rows that exist already are all settled (APPROVED, DECLINED,
-- REFUNDED), so they get 0 attempts and no next attempt.
alter table payments add column attempts integer not null default 0;
alter table payments add column next_attempt_at timestamp(6) with time zone;
alter table payments add column last_error varchar(500);

-- The worker reads "pending and due, oldest first" every second.
create index idx_payments_next_attempt on payments (next_attempt_at);
