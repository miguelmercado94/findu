-- =====================================================================
-- FINDU Core — Schema para PostgreSQL (perfil dev)
-- DROP + CREATE en cada arranque
-- =====================================================================

DROP TABLE IF EXISTS calificacion CASCADE;
DROP TABLE IF EXISTS factura_detalle CASCADE;
DROP TABLE IF EXISTS factura CASCADE;
DROP TABLE IF EXISTS oferta CASCADE;
DROP TABLE IF EXISTS solicitud_servicio CASCADE;
DROP TABLE IF EXISTS direccion CASCADE;
DROP TABLE IF EXISTS proveedor_cobertura CASCADE;
DROP TABLE IF EXISTS portafolio_item CASCADE;
DROP TABLE IF EXISTS perfil_especialista CASCADE;
DROP TABLE IF EXISTS perfil_proveedor CASCADE;
DROP TABLE IF EXISTS perfil_cliente CASCADE;
DROP TABLE IF EXISTS servicio CASCADE;
DROP TABLE IF EXISTS categoria CASCADE;
DROP TABLE IF EXISTS municipio CASCADE;

CREATE TABLE municipio (
    id BIGSERIAL PRIMARY KEY,
    codigo_dane VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    categoria_padre_id BIGINT REFERENCES categoria(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE servicio (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    tipo_cobro VARCHAR(20) NOT NULL DEFAULT 'POR_HORA',
    categoria_id BIGINT NOT NULL REFERENCES categoria(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE perfil_cliente (
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
    url_imagen_perfil VARCHAR(500),
    calificacion_promedio DECIMAL(3,2) DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE perfil_proveedor (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT NOT NULL UNIQUE,
    nombre_completo VARCHAR(200) NOT NULL,
    numero_identificacion VARCHAR(30) NOT NULL,
    tipo_identificacion VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    sexo VARCHAR(10),
    celular VARCHAR(20) NOT NULL,
    cod_phone_international VARCHAR(5) DEFAULT '+57',
    url_imagen_perfil VARCHAR(500),
    calificacion_promedio DECIMAL(3,2) DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    estado_verificacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE perfil_especialista (
    id BIGSERIAL PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    servicio_id BIGINT NOT NULL REFERENCES servicio(id),
    descripcion VARCHAR(500),
    experiencia_anios INT DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portafolio_item (
    id BIGSERIAL PRIMARY KEY,
    perfil_especialista_id BIGINT NOT NULL REFERENCES perfil_especialista(id),
    titulo VARCHAR(100),
    descripcion VARCHAR(500),
    url_imagen VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proveedor_cobertura (
    id BIGSERIAL PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    municipio_id BIGINT NOT NULL REFERENCES municipio(id)
);

CREATE TABLE direccion (
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

CREATE TABLE solicitud_servicio (
    id BIGSERIAL PRIMARY KEY,
    perfil_cliente_id BIGINT NOT NULL REFERENCES perfil_cliente(id),
    servicio_id BIGINT NOT NULL REFERENCES servicio(id),
    direccion_id BIGINT NOT NULL REFERENCES direccion(id),
    fecha_programada TIMESTAMP NOT NULL,
    nombre_contacto VARCHAR(100),
    telefono_contacto VARCHAR(20),
    prioridad INT DEFAULT 3,
    presupuesto_maximo DECIMAL(12,2),
    cantidad_estimada INT DEFAULT 1,
    estado_solicitud VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE oferta (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    perfil_proveedor_id BIGINT NOT NULL REFERENCES perfil_proveedor(id),
    valor_propuesto DECIMAL(12,2) NOT NULL,
    tiempo_estimado VARCHAR(50),
    mensaje_presentacion VARCHAR(1000),
    estado_oferta VARCHAR(30) NOT NULL DEFAULT 'ENVIADA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE factura (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL UNIQUE REFERENCES solicitud_servicio(id),
    subtotal DECIMAL(12,2) NOT NULL,
    comision_plataforma DECIMAL(12,2) DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE factura_detalle (
    id BIGSERIAL PRIMARY KEY,
    factura_id BIGINT NOT NULL REFERENCES factura(id),
    concepto VARCHAR(200) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    tipo VARCHAR(30) DEFAULT 'BASE',
    estado_aprobacion VARCHAR(30) DEFAULT 'APROBADO'
);

CREATE TABLE calificacion (
    id BIGSERIAL PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL REFERENCES solicitud_servicio(id),
    evaluador_id BIGINT NOT NULL,
    evaluado_id BIGINT NOT NULL,
    tipo_evaluacion VARCHAR(30) NOT NULL,
    puntaje INT NOT NULL CHECK (puntaje BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
