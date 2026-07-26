-- =============================================================================
-- Categorias — dado de referência (antes em import.sql).
--
-- ATENÇÃO: estes UUIDs são referenciados literalmente por DevDataLoader
-- (constantes CAT_TECNOLOGIA / CAT_FICCAO / CAT_NEGOCIOS). Alterá-los quebra
-- o seed de livros em dev. Ver bookland-app/.../DevDataLoader.java.
-- =============================================================================

insert into categories (id, name, active) values
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Ficção Científica',     true),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Romance',               true),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Tecnologia',            true),
    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'História',              true),
    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'Negócios',              true),
    ('f6a7b8c9-d0e1-2345-fabc-456789012345', 'Autoajuda',             true),
    ('a7b8c9d0-e1f2-3456-abcd-567890123456', 'Literatura Brasileira', true),
    ('b8c9d0e1-f2a3-4567-bcde-678901234567', 'Infantil',              true);
