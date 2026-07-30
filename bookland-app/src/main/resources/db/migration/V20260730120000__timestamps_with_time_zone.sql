-- =============================================================================
-- Bookland — colunas de data/hora passam a carregar fuso
--
-- Todo campo de data da API saía como LocalDateTime serializado direto
-- ("2026-07-30T13:01:34.4862"): uma string que não identifica um instante, e
-- que cada consumidor resolvia no fuso de onde o parse rodava. O mesmo dado
-- rendia 13:01 numa máquina em America/Sao_Paulo e 10:01 num container em UTC.
--
-- O tipo Java passou a ser Instant em todas as camadas, então a coluna precisa
-- acompanhar: timestamptz. Sem isso o ddl-auto: validate recusa o boot, que é
-- exatamente o comportamento desejado.
--
-- refresh_tokens já nasceu com fuso na V1 (o módulo auth sempre usou Instant) e
-- por isso não aparece aqui.
--
-- Sobre a forma do ALTER: "SET DATA TYPE" é o padrão SQL e vale nos dois
-- bancos; o "TYPE" solto é atalho exclusivo do PostgreSQL e quebraria o H2, que
-- roda esta mesma migração em dev.
--
-- Não há cláusula USING (que só existe no PostgreSQL): as tabelas estão vazias,
-- então não há linha para reinterpretar e nenhuma premissa de fuso fica
-- embutida no schema. Se um dia esta migração for aplicada sobre base com dado
-- gravado, as linhas antigas serão lidas no fuso da sessão do banco — o que
-- precisaria de uma conversão explícita, feita numa migração própria.
-- =============================================================================


-- --- catalog -----------------------------------------------------------------

alter table books alter column created_at set data type timestamp(6) with time zone;
alter table books alter column updated_at set data type timestamp(6) with time zone;


-- --- user --------------------------------------------------------------------

alter table users alter column created_at set data type timestamp(6) with time zone;
alter table users alter column updated_at set data type timestamp(6) with time zone;


-- --- orders ------------------------------------------------------------------

alter table carts alter column created_at set data type timestamp(6) with time zone;
alter table carts alter column updated_at set data type timestamp(6) with time zone;

alter table orders alter column created_at set data type timestamp(6) with time zone;
alter table orders alter column updated_at set data type timestamp(6) with time zone;

alter table order_status_transitions alter column changed_at set data type timestamp(6) with time zone;


-- --- payments ----------------------------------------------------------------

alter table payments alter column created_at set data type timestamp(6) with time zone;
alter table payments alter column updated_at set data type timestamp(6) with time zone;


-- --- reviews -----------------------------------------------------------------

alter table reviews alter column created_at set data type timestamp(6) with time zone;


-- --- wishlist ----------------------------------------------------------------

alter table wishlist_items alter column added_at set data type timestamp(6) with time zone;


-- --- inventory ---------------------------------------------------------------

alter table inventory_entries alter column adjusted_at set data type timestamp(6) with time zone;
