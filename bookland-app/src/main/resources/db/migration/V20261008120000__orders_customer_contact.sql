-- Who to tell about the order: the customer's email and name as the access token carried them at
-- checkout, stored on the order like the author's name on a review (V20261003120000), so the
-- notification service gets everything in the order's events and never asks the identity service.
-- Frozen at checkout on purpose. Same length as users.email/users.name.
--
-- Null on existing orders: there is nothing to backfill from here, the users live in another
-- database since step 3.
alter table orders add column customer_email varchar(255);
alter table orders add column customer_name varchar(255);
