-- =============================================================================
-- Bookland — as tabelas do identity saem do banco do monolito
--
-- users e as três oauth2_* pertencem agora ao serviço identity
-- (bookland-identity-app), com banco próprio e migration própria
-- (V20261006120000__identity_schema.sql, lá). Este processo não lê nem escreve
-- nenhuma delas: valida tokens pela chave pública do JWKS do identity.
--
-- Os dados NÃO são copiados (decisão do passo 3): o identity nasce vazio e os
-- seeds recriam admin e cliente de exemplo. Os customer_id já gravados em
-- orders, carts, reviews etc. ficam apontando para usuários que o novo banco
-- não conhece; recriar o ambiente é docker compose down -v.
--
-- Nenhuma FK de outro módulo aponta para estas tabelas (FKs só existem dentro
-- de um módulo), então o drop não esbarra em constraint.
-- =============================================================================

drop table oauth2_authorization_consent;
drop table oauth2_authorization;
drop table oauth2_registered_client;
drop table users;
