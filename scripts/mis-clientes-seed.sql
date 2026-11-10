-- Siembra DE DESARROLLO para "Mis clientes". NO ejecutar en produccion. Ajustar los ids a perfiles existentes.
-- Requiere haber creado las tablas (db/postgres/migrations/2026-mis-clientes.sql).
-- Alternativa por HTTP: POST /internal/dev/mis-clientes/vinculo con findu.dev-seed.enabled=true y X-Internal-Service-Key.

-- Vinculo activo: el cliente 1 tiene como favorito al proveedor 1.
INSERT INTO cliente_proveedor_favorito (perfil_cliente_id, perfil_proveedor_id, cliente_ref, activo, favorito_desde)
VALUES (1, 1, gen_random_uuid(), TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (perfil_cliente_id, perfil_proveedor_id) DO NOTHING;

-- Vinculo dado de baja: sigue en la lista del proveedor con chatEstado=BLOQUEADO.
INSERT INTO cliente_proveedor_favorito (perfil_cliente_id, perfil_proveedor_id, cliente_ref, activo, favorito_desde, removido_at)
VALUES (2, 1, gen_random_uuid(), FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (perfil_cliente_id, perfil_proveedor_id) DO NOTHING;

-- Marca privada del proveedor 1 sobre el cliente 1.
INSERT INTO proveedor_cliente_clasificacion (perfil_proveedor_id, perfil_cliente_id, favorito_interno)
VALUES (1, 1, TRUE)
ON CONFLICT (perfil_proveedor_id, perfil_cliente_id) DO NOTHING;