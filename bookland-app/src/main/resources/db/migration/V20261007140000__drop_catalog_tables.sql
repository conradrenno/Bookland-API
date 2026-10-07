-- =============================================================================
-- Bookland — as tabelas do catalog e do inventory saem do banco do monolito
--
-- Pertencem agora ao serviço catalog (bookland-catalog-app), com banco próprio
-- e migration própria (V20261007130000__catalog_schema.sql, lá). Este processo
-- lê livros por gRPC (BookCatalog.GetBooks) e fala de estoque pelo Kafka.
--
-- Os dados NÃO são copiados, como no passo 3: o catalog nasce com as categorias
-- da migration e os livros do seed de dev. Os book_id já gravados em carrinhos,
-- pedidos, wishlists e reviews ficam apontando para livros que o novo banco não
-- conhece (aparecem como "Unavailable"); recriar o ambiente é docker compose down -v.
--
-- Nenhuma FK de outro módulo aponta para estas tabelas. A ordem respeita as FKs
-- internas do catalog (itens → reservas, autores → livros → categorias).
-- =============================================================================

drop table stock_reservation_items;
drop table stock_reservations;
drop table book_authors;
drop table books;
drop table categories;
drop table inventory_entries;
drop table catalog_outbox;
drop table catalog_inbox;
