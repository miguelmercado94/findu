-- =====================================================================
-- FINDU Core — Datos iniciales para H2 (perfil local)
-- =====================================================================

-- Municipios de ejemplo (código DANE Colombia)
INSERT INTO municipio (codigo_dane, nombre, departamento) VALUES ('11001', 'Bogotá D.C.', 'Bogotá');
INSERT INTO municipio (codigo_dane, nombre, departamento) VALUES ('05001', 'Medellín', 'Antioquia');
INSERT INTO municipio (codigo_dane, nombre, departamento) VALUES ('76001', 'Cali', 'Valle del Cauca');
INSERT INTO municipio (codigo_dane, nombre, departamento) VALUES ('08001', 'Barranquilla', 'Atlántico');
INSERT INTO municipio (codigo_dane, nombre, departamento) VALUES ('13001', 'Cartagena', 'Bolívar');

-- Categorías de servicios
INSERT INTO categoria (nombre, descripcion) VALUES ('Salud y Bienestar', 'Servicios de salud, enfermería y cuidado personal');
INSERT INTO categoria (nombre, descripcion) VALUES ('Hogar', 'Servicios de mantenimiento y limpieza del hogar');
INSERT INTO categoria (nombre, descripcion) VALUES ('Belleza', 'Servicios de peluquería, manicure y estética');
INSERT INTO categoria (nombre, descripcion) VALUES ('Cuidado Personal', 'Niñeras, cuidadores de adultos mayores');
INSERT INTO categoria (nombre, descripcion) VALUES ('Tecnología', 'Soporte técnico, reparación de equipos');

-- Servicios
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Enfermería a Domicilio', 'Atención de enfermería profesional en casa', 'POR_HORA', 1);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Fisioterapia', 'Sesiones de rehabilitación física', 'POR_SESION', 1);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Limpieza General', 'Limpieza completa de hogar', 'POR_SERVICIO', 2);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Plomería', 'Reparación e instalación de tuberías', 'POR_SERVICIO', 2);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Corte de Cabello', 'Corte y peinado a domicilio', 'POR_SERVICIO', 3);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Manicure y Pedicure', 'Servicio de uñas a domicilio', 'POR_SERVICIO', 3);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Niñera', 'Cuidado de niños por horas', 'POR_HORA', 4);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Cuidador Adulto Mayor', 'Acompañamiento y cuidado de adultos mayores', 'POR_HORA', 4);
INSERT INTO servicio (nombre, descripcion, tipo_cobro, categoria_id) VALUES ('Soporte PC/Laptop', 'Diagnóstico y reparación de computadores', 'POR_SERVICIO', 5);
