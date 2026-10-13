-- Datos iniciales (PostgreSQL). Ejecutar después de schema.sql.
-- Requiere secuencias alineadas: los INSERT omiten ids; el orden respeta FKs.

-- Módulos: path_base = segmento del micro en la URI (igual que spring.webflux.base-path)
INSERT INTO module (name, path_base, active) VALUES ('AUTH', 'security-auth', TRUE);
INSERT INTO module (name, path_base, active) VALUES ('CUSTOMERS', 'security-auth', TRUE);
INSERT INTO module (name, path_base, active) VALUES ('PROFILE', 'security-auth', TRUE);

INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/login', 'AUTH_LOGIN', 'POST', 1, TRUE, TRUE); -- ID 1
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/federated', 'AUTH_FEDERATED', 'POST', 1, TRUE, TRUE); -- ID 2
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/refresh', 'AUTH_REFRESH', 'POST', 1, TRUE, TRUE); -- ID 3
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/logout', 'AUTH_LOGOUT', 'POST', 1, TRUE, TRUE); -- ID 4
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/validate', 'AUTH_VALIDATE', 'GET', 1, TRUE, TRUE); -- ID 5
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/customers', 'CUST_LIST', 'GET', 2, FALSE, TRUE); -- ID 6
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/customers', 'CUST_REGISTER', 'POST', 2, TRUE, TRUE); -- ID 7
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/profile', 'PROFILE_READ', 'GET', 3, FALSE, TRUE); -- ID 8
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/profile', 'PROFILE_UPDATE', 'PUT', 3, FALSE, TRUE); -- ID 9
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/forgot-password', 'AUTH_FORGOT_PASSWORD', 'POST', 1, TRUE, TRUE); -- ID 10
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/reset-password', 'AUTH_RESET_PASSWORD', 'POST', 1, TRUE, TRUE); -- ID 11
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/actuator/health', 'HEALTH_ACTUATOR', 'GET', 1, TRUE, TRUE); -- ID 12
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/public/health', 'PUBLIC_HEALTH', 'GET', 1, TRUE, TRUE); -- ID 13
-- Verificación de identidad del proveedor (FEAT-001). permite_all=TRUE: públicas por diseño (ver docs/verificacion-identidad-proveedor.md).
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/phone/send-otp', 'AUTH_PHONE_SEND_OTP', 'POST', 1, TRUE, TRUE); -- ID 14
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/phone/verify-otp', 'AUTH_PHONE_VERIFY_OTP', 'POST', 1, TRUE, TRUE); -- ID 15
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/email/send-verification', 'AUTH_EMAIL_SEND_VERIFICATION', 'POST', 1, TRUE, TRUE); -- ID 16
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/email/verify', 'AUTH_EMAIL_VERIFY', 'GET', 1, TRUE, TRUE); -- ID 17
-- Activación del proveedor (FEAT-001). Rutas ESTÁTICAS (el resolver de autorización compara path por
-- igualdad exacta); userId viaja en el body de approve/reject. module_id=3 (PROFILE), permite_all=FALSE.
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/activation/biometric', 'ACTIVATION_BIOMETRIC_SUBMIT', 'POST', 3, FALSE, TRUE); -- ID 18
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/activation/status', 'ACTIVATION_STATUS_READ', 'GET', 3, FALSE, TRUE); -- ID 19
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/activation/approve', 'ACTIVATION_APPROVE', 'POST', 3, FALSE, TRUE); -- ID 20
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/activation/reject', 'ACTIVATION_REJECT', 'POST', 3, FALSE, TRUE); -- ID 21

INSERT INTO role (name, active) VALUES ('ROLE_OUR_CLIENTE', TRUE); -- ID 1
INSERT INTO role (name, active) VALUES ('ROLE_ASSISTANT_ADMINISTRATOR', TRUE); -- ID 2
INSERT INTO role (name, active) VALUES ('ROLE_ADMINISTRATOR', TRUE); -- ID 3
INSERT INTO role (name, active) VALUES ('ROLE_OUR_PROVEEDOR', TRUE); -- ID 4

-- Operaciones para ROLE_OUR_CLIENTE (ID 1)
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 7, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 11, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 12, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 14, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 15, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 16, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 17, TRUE);

-- Operaciones para ROLE_ASSISTANT_ADMINISTRATOR (ID 2)
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 5, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 7, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 11, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 12, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 14, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 15, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 16, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 17, TRUE);

-- Operaciones para ROLE_ADMINISTRATOR (ID 3)
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 5, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 7, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 11, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 12, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 14, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 15, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 16, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 17, TRUE);

-- Operaciones para ROLE_OUR_PROVEEDOR (ID 4)
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 7, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 11, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 12, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 14, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 15, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 16, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 17, TRUE);
-- Activación: biometric + status para el proveedor (approve/reject son de soporte, rol 5 en add_soporte.sql).
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 18, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (4, 19, TRUE);

-- ---------------------------------------------------------------------------
-- Usuarios: NO se siembran. La base arranca SIN usuarios para probar el flujo
-- de registro + verificación (OTP celular + enlace de correo) desde cero.
-- Módulos, operaciones, roles y rol_operation SÍ se siembran (arriba), porque
-- la autorización depende de ellos.
-- Para re-crear usuarios de ejemplo, añadir aquí los INSERT correspondientes.
-- ---------------------------------------------------------------------------

