-- Datos iniciales (PostgreSQL). Ejecutar después de schema.sql.
-- Requiere secuencias alineadas: los INSERT omiten ids; el orden respeta FKs.

-- Módulos: path_base = segmento del micro en la URI (igual que spring.webflux.base-path)
INSERT INTO module (name, path_base, active) VALUES ('AUTH', 'security-auth', TRUE);
INSERT INTO module (name, path_base, active) VALUES ('CUSTOMERS', 'security-auth', TRUE);
INSERT INTO module (name, path_base, active) VALUES ('PROFILE', 'security-auth', TRUE);

INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/login', 'AUTH_LOGIN', 'POST', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/refresh', 'AUTH_REFRESH', 'POST', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/logout', 'AUTH_LOGOUT', 'POST', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/validate', 'AUTH_VALIDATE', 'GET', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/customers', 'CUST_LIST', 'GET', 2, FALSE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/customers', 'CUST_REGISTER', 'POST', 2, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/profile', 'PROFILE_READ', 'GET', 3, FALSE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/forgot-password', 'AUTH_FORGOT_PASSWORD', 'POST', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/v1/auth/reset-password', 'AUTH_RESET_PASSWORD', 'POST', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/actuator/health', 'HEALTH_ACTUATOR', 'GET', 1, TRUE, TRUE);
INSERT INTO operation (path, name, http_method, module_id, permite_all, active) VALUES ('/api/public/health', 'PUBLIC_HEALTH', 'GET', 1, TRUE, TRUE);

INSERT INTO role (name, active) VALUES ('ROLE_CUSTOMER', TRUE);
INSERT INTO role (name, active) VALUES ('ROLE_ASSISTANT_ADMINISTRATOR', TRUE);
INSERT INTO role (name, active) VALUES ('ROLE_ADMINISTRATOR', TRUE);

INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 7, TRUE);

INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 5, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 7, TRUE);

INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 1, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 2, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 3, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 4, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 5, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 6, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 7, TRUE);

INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (1, 11, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (2, 11, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 8, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 9, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 10, TRUE);
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (3, 11, TRUE);

INSERT INTO "user" (phone, username, password, email, active) VALUES (NULL, 'lmarquez', '$2a$10$ywh1O2EwghHmFIMGeHgsx.9lMw5IXpg4jafeFS.Oi6nFv0181gHli', 'lmarquez@example.com', TRUE);
INSERT INTO "user" (phone, username, password, email, active) VALUES (NULL, 'fperez', '$2a$10$V29z7/qC9wpHfzRMxGOHye5RMAxCid2/MzJalk0dsiA3zZ9CJfub.', 'fperez@example.com', TRUE);
INSERT INTO "user" (phone, username, password, email, active) VALUES (NULL, 'mhernandez', '$2a$10$TMbMuEZ8utU5iq8MOoxpmOc6QWQuYuwgx1xJF8lSMNkKP3hIrwYFG', 'mhernandez@example.com', TRUE);

INSERT INTO user_rol (role_id, user_id, active) VALUES (1, 1, TRUE);
INSERT INTO user_rol (role_id, user_id, active) VALUES (2, 2, TRUE);
INSERT INTO user_rol (role_id, user_id, active) VALUES (3, 3, TRUE);
