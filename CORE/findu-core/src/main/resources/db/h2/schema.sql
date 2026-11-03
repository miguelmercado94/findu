-- =====================================================================
-- FINDU Core — Schema para H2 (perfil local)
-- Ejecutado en cada arranque (spring.sql.init.mode=always)
-- =====================================================================

DROP TABLE IF EXISTS amonestacion_proveedor;
DROP TABLE IF EXISTS cancelacion_proveedor;
DROP TABLE IF EXISTS solicitud_estado_auditoria;
DROP TABLE IF EXISTS calificacion;
DROP TABLE IF EXISTS factura_detalle;
DROP TABLE IF EXISTS factura;
DROP TABLE IF EXISTS oferta_cronograma_dia;
DROP TABLE IF EXISTS oferta;
DROP TABLE IF EXISTS solicitud_servicio;
DROP TABLE IF EXISTS direccion;
DROP TABLE IF EXISTS proveedor_cobertura;
DROP TABLE IF EXISTS especialista_credenciales;
DROP TABLE IF EXISTS portafolio_item;
DROP TABLE IF EXISTS perfil_especialista;
DROP TABLE IF EXISTS perfil_proveedor;
DROP TABLE IF EXISTS perfil_cliente;
DROP TABLE IF EXISTS servicio;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS municipio;

-- ─── Catálogos ────────────────────────────────────────────────────────

CREATE TABLE municipio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_dane VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    categoria_padre_id BIGINT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_padre_id) REFERENCES categoria(id)
);

CREATE TABLE servicio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    tipo_cobro VARCHAR(20) NOT NULL DEFAULT 'POR_HORA',
    categoria_id BIGINT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

-- ─── Perfiles ─────────────────────────────────────────────────────────

CREATE TABLE perfil_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE perfil_proveedor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    estado_verificacion VARCHAR(30) DEFAULT 'PENDIENTE',
    disponible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE perfil_especialista (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    descripcion VARCHAR(500),
    experiencia_anios INT DEFAULT 0,
    calificacion_promedio DECIMAL(3,2) DEFAULT 0.00,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (perfil_proveedor_id) REFERENCES perfil_proveedor(id),
    FOREIGN KEY (servicio_id) REFERENCES servicio(id)
);

CREATE TABLE portafolio_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_especialista_id BIGINT NOT NULL,
    titulo VARCHAR(100),
    descripcion VARCHAR(500),
    url_imagen TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (perfil_especialista_id) REFERENCES perfil_especialista(id)
);

CREATE TABLE especialista_credenciales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_especialista_id BIGINT NOT NULL,
    tipo_certificado VARCHAR(30) NOT NULL,
    nombre_titulo VARCHAR(200) NOT NULL,
    institucion VARCHAR(200) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE NOT NULL,
    url_certificado_s3 TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (perfil_especialista_id) REFERENCES perfil_especialista(id)
);

CREATE TABLE proveedor_cobertura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL,
    municipio_id BIGINT NOT NULL,
    FOREIGN KEY (perfil_proveedor_id) REFERENCES perfil_proveedor(id),
    FOREIGN KEY (municipio_id) REFERENCES municipio(id)
);

-- ─── Direcciones ──────────────────────────────────────────────────────

CREATE TABLE direccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_cliente_id BIGINT,
    perfil_proveedor_id BIGINT,
    solicitud_servicio_id BIGINT,
    etiqueta VARCHAR(50),
    direccion_texto VARCHAR(300) NOT NULL,
    municipio_id BIGINT NOT NULL,
    latitud DECIMAL(10,7),
    longitud DECIMAL(10,7),
    piso VARCHAR(20),
    apartamento VARCHAR(20),
    referencia VARCHAR(200),
    es_principal BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (perfil_cliente_id) REFERENCES perfil_cliente(id),
    FOREIGN KEY (perfil_proveedor_id) REFERENCES perfil_proveedor(id),
    FOREIGN KEY (municipio_id) REFERENCES municipio(id)
);

-- ─── Solicitudes y Ofertas ────────────────────────────────────────────

CREATE TABLE solicitud_servicio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_cliente_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    direccion_id BIGINT NOT NULL,
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
    estado_solicitud VARCHAR(30) DEFAULT 'ABIERTA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (perfil_cliente_id) REFERENCES perfil_cliente(id),
    FOREIGN KEY (servicio_id) REFERENCES servicio(id),
    FOREIGN KEY (direccion_id) REFERENCES direccion(id)
);

CREATE TABLE oferta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL,
    perfil_proveedor_id BIGINT NOT NULL,
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
    estado_oferta VARCHAR(30) DEFAULT 'ENVIADA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (solicitud_servicio_id) REFERENCES solicitud_servicio(id),
    FOREIGN KEY (perfil_proveedor_id) REFERENCES perfil_proveedor(id)
);

CREATE TABLE oferta_cronograma_dia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    oferta_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    dia_semana VARCHAR(20) NOT NULL,
    hora_inicio TIME,
    hora_fin TIME,
    horas_trabajo DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (oferta_id) REFERENCES oferta(id) ON DELETE CASCADE
);

-- ─── Facturación ──────────────────────────────────────────────────────

CREATE TABLE factura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL UNIQUE,
    subtotal DECIMAL(12,2) NOT NULL,
    comision_plataforma DECIMAL(12,2) DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) DEFAULT 'PENDIENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (solicitud_servicio_id) REFERENCES solicitud_servicio(id)
);

CREATE TABLE factura_detalle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    factura_id BIGINT NOT NULL,
    concepto VARCHAR(200) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    tipo VARCHAR(30) DEFAULT 'BASE',
    FOREIGN KEY (factura_id) REFERENCES factura(id)
);

-- ─── Calificaciones ───────────────────────────────────────────────────

CREATE TABLE calificacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL,
    evaluador_id BIGINT NOT NULL,
    evaluado_id BIGINT NOT NULL,
    tipo_evaluacion VARCHAR(30) NOT NULL,
    puntaje INT NOT NULL CHECK (puntaje BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (solicitud_servicio_id) REFERENCES solicitud_servicio(id)
);

-- ─── Auditoría Interna GPS/Estado, Cancelaciones y Amonestaciones ─────

CREATE TABLE solicitud_estado_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL,
    estado_anterior VARCHAR(40),
    estado_nuevo VARCHAR(40) NOT NULL,
    actor_tipo VARCHAR(30),
    actor_id BIGINT,
    latitud DECIMAL(10,7),
    longitud DECIMAL(10,7),
    distancia_metros DECIMAL(10,2),
    observacion VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (solicitud_servicio_id) REFERENCES solicitud_servicio(id)
);

CREATE TABLE cancelacion_proveedor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_servicio_id BIGINT NOT NULL,
    oferta_id BIGINT,
    perfil_proveedor_id BIGINT NOT NULL,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (solicitud_servicio_id) REFERENCES solicitud_servicio(id),
    FOREIGN KEY (oferta_id) REFERENCES oferta(id),
    FOREIGN KEY (perfil_proveedor_id) REFERENCES perfil_proveedor(id)
);

CREATE TABLE amonestacion_proveedor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_proveedor_id BIGINT NOT NULL,
    solicitud_servicio_id BIGINT NOT NULL,
    cancelacion_id BIGINT NOT NULL UNIQUE,
    ticket_codigo VARCHAR(60),
    nivel VARCHAR(20) NOT NULL,
    motivo VARCHAR(300) NOT NULL,
    conteo_30_dias INT NOT NULL,
    dias_restriccion INT NOT NULL DEFAULT 0,
    restriccion_inicio TIMESTAMP,
    restriccion_fin TIMESTAMP,
    activa BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (perfil_proveedor_id) REFERENCES perfil_proveedor(id),
    FOREIGN KEY (solicitud_servicio_id) REFERENCES solicitud_servicio(id),
    FOREIGN KEY (cancelacion_id) REFERENCES cancelacion_proveedor(id)
);
