-- Este archivo es una copia de referencia.
-- Al levantar el proyecto se ejecutan: src/main/resources/schema.sql (DROP + CREATE) y src/main/resources/data.sql (estos INSERT).
-- La configuración está en application.yml (spring.sql.init.mode=always).

-- Roles (deben existir antes de user_rol)
INSERT INTO role (name, active) VALUES ('ROLE_CUSTOMER', TRUE);
INSERT INTO role (name, active) VALUES ('ROLE_ASSISTANT_ADMINISTRATOR', TRUE);
INSERT INTO role (name, active) VALUES ('ROLE_ADMINISTRATOR', TRUE);

-- Usuarios (tabla "user": phone, username, password, email, active)
INSERT INTO "user" (phone, username, password, email, active) VALUES (NULL, 'lmarquez', '$2a$10$ywh1O2EwghHmFIMGeHgsx.9lMw5IXpg4jafeFS.Oi6nFv0181gHli', 'lmarquez@example.com', TRUE);
INSERT INTO "user" (phone, username, password, email, active) VALUES (NULL, 'fperez', '$2a$10$V29z7/qC9wpHfzRMxGOHye5RMAxCid2/MzJalk0dsiA3zZ9CJfub.', 'fperez@example.com', TRUE);
INSERT INTO "user" (phone, username, password, email, active) VALUES (NULL, 'mhernandez', '$2a$10$TMbMuEZ8utU5iq8MOoxpmOc6QWQuYuwgx1xJF8lSMNkKP3hIrwYFG', 'mhernandez@example.com', TRUE);

-- user_rol (role_id, user_id, active) — asocia usuarios con roles (ids 1,2,3 para users e ids 1,2,3 para roles)
INSERT INTO user_rol (role_id, user_id, active) VALUES (1, 1, TRUE);
INSERT INTO user_rol (role_id, user_id, active) VALUES (2, 2, TRUE);
INSERT INTO user_rol (role_id, user_id, active) VALUES (3, 3, TRUE);
