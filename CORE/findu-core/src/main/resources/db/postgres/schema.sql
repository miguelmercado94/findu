-- =====================================================================
-- FINDU Core — Schema para PostgreSQL (perfil dev)
-- Tablas persistentes con IF NOT EXISTS para evitar pérdida de datos en reinicio.
-- =====================================================================

CREATE TABLE IF NOT EXISTS municipio (
    id BIGSERIAL PRIMARY KEY,
    codigo_dane VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS categoria (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    categoria_padre_id BIGINT REFERENCES categoria(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS servicio (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    tipo_cobro VARCHAR(20) NOT NULL DEFAULT 'POR_HORA',
    categoria_id BIGINT NOT NULL REFERENCES categoria(id),
    url_imagen TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS perfil_cliente (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    nombre_completo VARCHAR(200) NOT NULL,
    numero_identificacion VARCHAR(30) NOT NULL,
    tipo_identificacion VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    sexo VARCHAR(10),
    celular VARCHAR(20) NOT NULL,
    cod_phone_international VARCHAR(5) DEFAULT '+57',
    url_imagen_perfil TEXT,
    calificacion_promedio DECIMAL(3,2) DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS perfil_proveedor (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT NOT NULL UNIQUE,
    nombre_completo VARCHAR(200) NOT NULL,
    numero_identificacion VARCHAR(30) NOT NULL,
    tipo_identificacion VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    sexo VARCHAR(10),
    celular VARCHAR(20) NOT NULL,
    cod_phone_international VARCHAR(5) DEFAULT '+57',
    url_imagen_perfil TEXT,
    calificacion_promedio DECIMAL(3,2) DEFAULT 5.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    estado_verificacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    biometria_verificada_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Espejo de biometria completada (solo lo escribe el endpoint interno de biometria). Idempotente para BDs existentes.
ALTER TABLE perfil_proveedor ADD COLUMN IF NOT EXISTS biometria_verificada_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS perfil_especialista (
    id BIGSERIAL PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    servicio_id BIGINT NOT NULL REFERENCES servicio(id),
    descripcion VARCHAR(500),
    experiencia_anios INT DEFAULT 0,
    calificacion_promedio DECIMAL(3,2) DEFAULT 5.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS portafolio_item (
    id BIGSERIAL PRIMARY KEY,
    perfil_especialista_id BIGINT NOT NULL REFERENCES perfil_especialista(id),
    titulo VARCHAR(100),
    descripcion VARCHAR(500),
    url_imagen TEXT,
    url_folder_imagen TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS especialista_credenciales (
    id BIGSERIAL PRIMARY KEY,
    perfil_especialista_id BIGINT NOT NULL REFERENCES perfil_especialista(id),
    tipo_certificado VARCHAR(30) NOT NULL,
    nombre_titulo VARCHAR(200) NOT NULL,
    institucion VARCHAR(200) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE NOT NULL,
    url_certificado_s3 TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proveedor_cobertura (
    id BIGSERIAL PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    municipio_id BIGINT NOT NULL REFERENCES municipio(id)
);

CREATE TABLE IF NOT EXISTS direccion (
    id BIGSERIAL PRIMARY KEY,
    perfil_cliente_id BIGINT REFERENCES perfil_cliente(id),
    perfil_proveedor_id BIGINT REFERENCES perfil_proveedor(id),
    solicitud_servicio_id BIGINT,
    etiqueta VARCHAR(50),
    direccion_texto VARCHAR(300) NOT NULL,
    municipio_id BIGINT NOT NULL REFERENCES municipio(id),
    latitud DECIMAL(10,7),
    longitud DECIMAL(10,7),
    piso VARCHAR(20),
    apartamento VARCHAR(20),
    referencia VARCHAR(200),
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS solicitud_servicio (
    id BIGSERIAL PRIMARY KEY,
    perfil_cliente_id BIGINT NOT NULL REFERENCES perfil_cliente(id),
    servicio_id BIGINT NOT NULL REFERENCES servicio(id),
    direccion_id BIGINT NOT NULL REFERENCES direccion(id),
    fecha_programada TIMESTAMP NOT NULL,
    nombre_contacto VARCHAR(100),
    telefono_contacto VARCHAR(20),
    prioridad INT DEFAULT 3,
    presupuesto_maximo DECIMAL(12,2),
    es_presupuesto_estricto BOOLEAN DEFAULT FALSE,
    cantidad_estimada INT DEFAULT 1,
    detalles TEXT,
    fotos TEXT,
    pin_inicio_encrypted VARCHAR(255),
    recordatorio_1h_enviado BOOLEAN DEFAULT FALSE,
    estado_solicitud VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS oferta (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    valor_propuesto DECIMAL(12,2) NOT NULL,
    hora_inicio_propuesta TIME,
    tiempo_estimado VARCHAR(50),
    mensaje_presentacion VARCHAR(1000),
    duracion_unidad VARCHAR(20) NOT NULL DEFAULT 'HORAS',
    duracion_cantidad DECIMAL(10,2) NOT NULL DEFAULT 1.00,
    mismo_horario_dias BOOLEAN DEFAULT FALSE,
    ofertado_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    solicitud_modificada BOOLEAN DEFAULT FALSE,
    campos_modificados TEXT,
    valores_anteriores TEXT,
    solapamiento_horario BOOLEAN DEFAULT FALSE,
    estado_oferta VARCHAR(30) NOT NULL DEFAULT 'ENVIADA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS oferta_cronograma_dia (
    id BIGSERIAL PRIMARY KEY,
    oferta_id BIGINT NOT NULL REFERENCES oferta(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    dia_semana VARCHAR(20) NOT NULL,
    hora_inicio TIME,
    hora_fin TIME,
    horas_trabajo DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS factura (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL UNIQUE REFERENCES solicitud_servicio(id),
    subtotal DECIMAL(12,2) NOT NULL,
    comision_plataforma DECIMAL(12,2) DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS factura_detalle (
    id BIGSERIAL PRIMARY KEY,
    factura_id BIGINT NOT NULL REFERENCES factura(id),
    concepto VARCHAR(200) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    tipo VARCHAR(30) DEFAULT 'BASE',
    estado_aprobacion VARCHAR(30) DEFAULT 'APROBADO'
);

CREATE TABLE IF NOT EXISTS calificacion (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    evaluador_id BIGINT NOT NULL,
    evaluado_id BIGINT NOT NULL,
    tipo_evaluacion VARCHAR(30) NOT NULL,
    puntaje INT NOT NULL CHECK (puntaje BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─── Auditoría Interna GPS/Estado, Cancelaciones y Amonestaciones ─────

CREATE TABLE IF NOT EXISTS solicitud_estado_auditoria (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    estado_anterior VARCHAR(40),
    estado_nuevo VARCHAR(40) NOT NULL,
    actor_tipo VARCHAR(30),
    actor_id BIGINT,
    latitud DECIMAL(10,7),
    longitud DECIMAL(10,7),
    distancia_metros DECIMAL(10,2),
    observacion VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cancelacion_proveedor (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    oferta_id BIGINT REFERENCES oferta(id),
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    categoria VARCHAR(40) NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    descripcion TEXT,
    evidencias_urls TEXT,
    ticket_codigo VARCHAR(60),
    estado_cancelacion VARCHAR(40) NOT NULL,
    estado_ticket VARCHAR(40) NOT NULL,
    es_injustificada BOOLEAN,
    fecha_limite_evidencia TIMESTAMP,
    decision_soporte VARCHAR(40),
    justificacion_soporte TEXT,
    agente_soporte_id BIGINT,
    fecha_resolucion TIMESTAMP,
    amonestacion_generada BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS amonestacion_proveedor (
    id BIGSERIAL PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    cancelacion_id BIGINT NOT NULL UNIQUE REFERENCES cancelacion_proveedor(id),
    ticket_codigo VARCHAR(60),
    nivel VARCHAR(20) NOT NULL,
    motivo VARCHAR(300) NOT NULL,
    conteo_30_dias INT NOT NULL,
    dias_restriccion INT NOT NULL DEFAULT 0,
    restriccion_inicio TIMESTAMP,
    restriccion_fin TIMESTAMP,
    activa BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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