-- =============================================================================
-- Bookland — tabelas do Spring Authorization Server
--
-- As três tabelas saem dos scripts oficiais de dentro do jar
-- spring-security-oauth2-authorization-server-7.0.5, em
-- org/springframework/security/oauth2/server/authorization/. Nada aqui é
-- invenção nossa: o JdbcRegisteredClientRepository, o JdbcOAuth2AuthorizationService
-- e o JdbcOAuth2AuthorizationConsentService leem e escrevem exatamente estes
-- nomes de coluna via JdbcTemplate. Renomear qualquer um quebra o framework.
--
-- Duas adaptações, ambas mandadas pelo cabeçalho do próprio script oficial:
--   * blob      -> text                         (só ocorre em oauth2_authorization)
--   * timestamp -> timestamp with time zone     (nas duas tabelas que têm datas)
--
-- Os dois tipos existem no PostgreSQL e no H2 em MODE=PostgreSQL, então o SQL
-- continua no subconjunto comum que este projeto exige. A forma "timestamp(6)
-- with time zone" é a mesma já usada na V20260730120000.
--
-- Nenhuma @Entity mapeia estas tabelas — o Authorization Server é JdbcTemplate
-- puro. Portanto não há drift para o ddl-auto: validate reclamar, e o
-- RegisteredClient continua fora do domínio (decisão D4 do plano da Fase 1).
--
-- Sobre FKs: oauth2_authorization.registered_client_id e
-- oauth2_authorization_consent.registered_client_id apontam para
-- oauth2_registered_client, e o script oficial não declara constraint nenhuma.
-- Mantido como está — as três tabelas são internas ao Authorization Server e
-- viajam juntas na extração da Fase 2.
-- =============================================================================


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
-- Server, fora do contrato do token; só saiba que "listar autorizações do
-- usuário X" se consulta por e-mail (seção 3.3 do plano da Fase 1).

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
