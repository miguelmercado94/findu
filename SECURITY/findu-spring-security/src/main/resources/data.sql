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

INSERT INTO "user" (phone, cod_phone_international, username, password, email, active) VALUES ('3003763300', '+57', 'lmarquez', '$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO', 'lmarquez@example.com', TRUE);
INSERT INTO "user" (phone, cod_phone_international, username, password, email, active) VALUES ('3115551234', '+57', 'fperez', '$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO', 'fperez@example.com', TRUE);
INSERT INTO "user" (phone, cod_phone_international, username, password, email, active) VALUES ('3209876543', '+57', 'mhernandez', '$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO', 'mhernandez@example.com', TRUE);

-- lmarquez tendrá el rol de Cliente (ID 1) y de Proveedor (ID 4) simultáneamente
INSERT INTO user_rol (role_id, user_id, active) VALUES (1, 1, TRUE);
INSERT INTO user_rol (role_id, user_id, active) VALUES (4, 1, TRUE);

-- fperez es Asistente (ID 2)
INSERT INTO user_rol (role_id, user_id, active) VALUES (2, 2, TRUE);

-- mhernandez es Administrador (ID 3)
INSERT INTO user_rol (role_id, user_id, active) VALUES (3, 3, TRUE);
