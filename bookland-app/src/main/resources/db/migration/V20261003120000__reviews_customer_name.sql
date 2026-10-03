-- The review carries its author's display name, written once when the review is created, so that
-- listing reviews no longer asks the user module for each author.
alter table reviews add column customer_name varchar(255);

-- Backfill for the reviews that already exist. This reads another module's table, which is only
-- possible while both modules share one database: it has to happen before the user module is
-- extracted into its own service. Same length as users.name, so every name fits.
update reviews
   set customer_name = (select u.name from users u where u.id = reviews.customer_id);
