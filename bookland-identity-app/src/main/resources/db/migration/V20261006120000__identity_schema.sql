-- =============================================================================
-- Bookland Identity — schema inicial do serviço
--
-- O estado ATUAL das tabelas que o monolito criou ao longo de várias migrations
-- (V20260726164500, V20260730120000 e V20260810093000), consolidado num arquivo
-- só: este banco nasce agora, não tem história para reproduzir.
--
-- users: como a init_schema a criou, já com os timestamps "with time zone" da
-- V20260730120000. A coluna active existe desde o início.
--
-- oauth2_*: idênticas à V20260810093000 do monolito, que copia os scripts
-- oficiais do jar spring-security-oauth2-authorization-server com duas
-- adaptações (blob -> text, timestamp -> timestamp with time zone). Os
-- comentários de lá valem aqui; os essenciais estão repetidos abaixo.
--
-- O SQL segue no subconjunto comum PostgreSQL/H2 (MODE=PostgreSQL), como no
-- monolito: o dev roda estas mesmas migrations contra um H2 em memória.
-- =============================================================================


-- --- users -------------------------------------------------------------------

create table users (
    id            uuid         not null,
    name          varchar(255) not null,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(255) not null check (role in ('CUSTOMER', 'ADMIN')),
    active        boolean      not null,
    created_at    timestamp(6) with time zone not null,
    updated_at    timestamp(6) with time zone not null,
    primary key (id)
);


-- --- clients registrados -----------------------------------------------------
-- A linha é criada por um bootstrap idempotente a partir de properties, não por
-- SQL: redirect URI e secret mudam por ambiente (decisão D5).

create table oauth2_registered_client (
    id                            varchar(100)  not null,
    client_id                     varchar(100)  not null,
    client_id_issued_at           timestamp(6) with time zone default current_timestamp not null,
    client_secret                 varchar(200)  default null,
    client_secret_expires_at      timestamp(6) with time zone default null,
    client_name                   varchar(200)  not null,
    client_authentication_methods varchar(1000) not null,
    authorization_grant_types     varchar(1000) not null,
    redirect_uris                 varchar(1000) default null,
    post_logout_redirect_uris     varchar(1000) default null,
    scopes                        varchar(1000) not null,
    client_settings               varchar(2000) not null,
    token_settings                varchar(2000) not null,
    primary key (id)
);


-- --- autorizações emitidas ---------------------------------------------------
-- Uma linha por autorização viva: guarda o authorization code, o access token, o
-- id_token e o refresh token de uma mesma sessão OAuth2.
--
-- principal_name recebe Authentication.getName(), que no nosso caso é o e-mail —
-- e não o UserId que vai no claim sub. É bookkeeping interno do Authorization
-- Server, fora do contrato do token.

create table oauth2_authorization (
    id                            varchar(100)  not null,
    registered_client_id          varchar(100)  not null,
    principal_name                varchar(200)  not null,
    authorization_grant_type      varchar(100)  not null,
    authorized_scopes             varchar(1000) default null,
    attributes                    text          default null,
    state                         varchar(500)  default null,
    authorization_code_value      text          default null,
    authorization_code_issued_at  timestamp(6) with time zone default null,
    authorization_code_expires_at timestamp(6) with time zone default null,
    authorization_code_metadata   text          default null,
    access_token_value            text          default null,
    access_token_issued_at        timestamp(6) with time zone default null,
    access_token_expires_at       timestamp(6) with time zone default null,
    access_token_metadata         text          default null,
    access_token_type             varchar(100)  default null,
    access_token_scopes           varchar(1000) default null,
    oidc_id_token_value           text          default null,
    oidc_id_token_issued_at       timestamp(6) with time zone default null,
    oidc_id_token_expires_at      timestamp(6) with time zone default null,
    oidc_id_token_metadata        text          default null,
    refresh_token_value           text          default null,
    refresh_token_issued_at       timestamp(6) with time zone default null,
    refresh_token_expires_at      timestamp(6) with time zone default null,
    refresh_token_metadata        text          default null,
    user_code_value               text          default null,
    user_code_issued_at           timestamp(6) with time zone default null,
    user_code_expires_at          timestamp(6) with time zone default null,
    user_code_metadata            text          default null,
    device_code_value             text          default null,
    device_code_issued_at         timestamp(6) with time zone default null,
    device_code_expires_at        timestamp(6) with time zone default null,
    device_code_metadata          text          default null,
    primary key (id)
);


-- --- consentimentos ----------------------------------------------------------
-- Fica vazia na Fase 1: o client é first-party e nasce com
-- requireAuthorizationConsent(false). A tabela existe agora para não precisar de
-- uma segunda migração quando um client third-party aparecer (decisão D6).

create table oauth2_authorization_consent (
    registered_client_id varchar(100)  not null,
    principal_name       varchar(200)  not null,
    authorities          varchar(1000) not null,
    primary key (registered_client_id, principal_name)
);


-- --- índices -----------------------------------------------------------------
-- O script oficial não cria nenhum, e o JdbcOAuth2AuthorizationService busca a
-- autorização pelo VALOR do token. Sem índice, cada troca de code e cada refresh
-- é full scan em oauth2_authorization — a tabela que mais cresce.
--
-- Só as colunas de valor curto entram. Todas guardam um base64 de 96 bytes
-- gerado pelo próprio servidor (~128 caracteres), muito abaixo do limite de
-- ~2704 bytes por entrada de índice btree do PostgreSQL.
--
-- access_token_value e oidc_id_token_value ficam DELIBERADAMENTE de fora: são
-- JWTs, cujo tamanho cresce com os claims, e um token acima daquele limite faria
-- o INSERT falhar — a autorização deixaria de ser gravada no meio de um login
-- que já deu certo. Nenhum fluxo da Fase 1 busca por eles: o Resource Server
-- valida o JWT localmente pela chave pública do JWKS, sem introspecção. Se
-- /oauth2/introspect ou /oauth2/revoke entrarem em uso, o índice para eles é uma
-- expressão sobre hash, não a coluna crua.
--
-- user_code_value e device_code_value são do device grant, que não usamos.

create unique index idx_oauth2_registered_client_client_id
    on oauth2_registered_client (client_id);

create index idx_oauth2_authorization_state
    on oauth2_authorization (state);
create index idx_oauth2_authorization_code_value
    on oauth2_authorization (authorization_code_value);
create index idx_oauth2_authorization_refresh_token_value
    on oauth2_authorization (refresh_token_value);

-- Suporta a consulta por (registered_client_id, principal_name) do
-- JdbcOAuth2AuthorizationService, e a expiração/limpeza por client.
create index idx_oauth2_authorization_client_principal
    on oauth2_authorization (registered_client_id, principal_name);
