INSERT INTO role (id, name, active) VALUES (5, 'ROLE_OUR_SOPORTE', TRUE) ON CONFLICT DO NOTHING;

INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 1, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 2, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 3, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 4, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 5, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 6, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 7, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 8, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 9, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 10, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 11, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO rol_operation (role_id, operation_id, active) VALUES (5, 12, TRUE) ON CONFLICT DO NOTHING;

INSERT INTO "user" (phone, cod_phone_international, username, password, email, estado, active) 
VALUES ('3009998877', '+57', 'soporte', '$2a$10$oNJfMQXy42j7xNkiPCOz9euYYh7A15AkKzgH0PQZ6Yoddp5gW9dmi', 'soporte@findu.com', 'COMPLETO', TRUE) 
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password;

INSERT INTO user_rol (role_id, user_id, active) 
SELECT 5, id, TRUE FROM "user" WHERE username = 'soporte' ON CONFLICT DO NOTHING;

INSERT INTO user_rol (role_id, user_id, active) 
SELECT 3, id, TRUE FROM "user" WHERE username = 'soporte' ON CONFLICT DO NOTHING;
