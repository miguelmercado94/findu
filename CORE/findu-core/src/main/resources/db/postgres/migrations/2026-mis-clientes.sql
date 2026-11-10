-- Migracion 2026: modulo Mis clientes. Idempotente. NO aplicada automaticamente (el proyecto no usa Flyway/Liquibase).

-- Mis clientes: vinculo cliente -> proveedor favorito (nunca se borra; activo=false = baja) y marca privada del proveedor.
CREATE TABLE IF NOT EXISTS cliente_proveedor_favorito (
    id BIGSERIAL PRIMARY KEY,
    perfil_cliente_id BIGINT NOT NULL REFERENCES perfil_cliente(id),
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    cliente_ref UUID NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    favorito_desde TIMESTAMP,
    removido_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cliente_proveedor_favorito UNIQUE (perfil_cliente_id, perfil_proveedor_id)
);
CREATE INDEX IF NOT EXISTS idx_cpf_proveedor_activo ON cliente_proveedor_favorito (perfil_proveedor_id, activo);

CREATE TABLE IF NOT EXISTS proveedor_cliente_clasificacion (
    id BIGSERIAL PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    perfil_cliente_id BIGINT NOT NULL REFERENCES perfil_cliente(id),
    favorito_interno BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_proveedor_cliente_clasificacion UNIQUE (perfil_proveedor_id, perfil_cliente_id)
);