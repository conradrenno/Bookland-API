-- One live review per customer and book, enforced by the database. The service checks before
-- inserting, but two submissions at the same moment both pass the check.
--
-- A plain unique (book_id, customer_id) would forbid reviewing again after a moderator removed the
-- review (soft delete), which is allowed; a partial index (where deleted = false) is not in H2, and
-- migrations stay in the PostgreSQL/H2 common subset. So: a column that holds the customer while the
-- review is live and null once it is removed. Both databases treat nulls as distinct in a unique
-- index, so removed reviews never collide.
alter table reviews add column live_customer_id uuid;

-- Backfill the live reviews. Should duplicates already exist (the race this closes), only the
-- newest of each pair is marked live, or creating the index would fail.
update reviews r
   set live_customer_id = r.customer_id
 where r.deleted = false
   and not exists (select 1 from reviews o
                    where o.book_id = r.book_id
                      and o.customer_id = r.customer_id
                      and o.deleted = false
                      and (o.created_at > r.created_at or (o.created_at = r.created_at and o.id > r.id)));

create unique index uk_reviews_live_review on reviews (book_id, live_customer_id);
