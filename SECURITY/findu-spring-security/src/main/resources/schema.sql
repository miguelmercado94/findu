-- DROP-CREATE: eliminar tablas en orden (hijos antes que padres)
DROP TABLE IF EXISTS user_rol;
DROP TABLE IF EXISTS rol_operation;
DROP TABLE IF EXISTS password_recovery_token;
DROP TABLE IF EXISTS "user";
DROP TABLE IF EXISTS operation;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS module;

-- module: path_base = segmento del micro en la URI (ej. security-auth), alineado con spring.webflux.base-path / descubrimiento
CREATE TABLE module (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(25) NOT NULL,
    path_base VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uq_module_name UNIQUE (name)
);

-- operation: path = resto de la URL tras el path_base del micro; http_method = GET, POST, …; name = authority Spring
CREATE TABLE operation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(255) NOT NULL,
    name VARCHAR(64),
    http_method VARCHAR(10) NOT NULL,
    module_id BIGINT NOT NULL,
    permite_all BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_operation_module FOREIGN KEY (module_id) REFERENCES module(id),
    CONSTRAINT uq_operation_name UNIQUE (name),
    CONSTRAINT uq_operation_module_path_method UNIQUE (module_id, path, http_method)
);

-- role: id (int), name, active, audit (name único)
CREATE TABLE role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uq_role_name UNIQUE (name)
);

-- rol_operation: N:N entre role y operation, active, audit
CREATE TABLE rol_operation (
    role_id INT NOT NULL,
    operation_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (role_id, operation_id),
    CONSTRAINT fk_rol_operation_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_rol_operation_operation FOREIGN KEY (operation_id) REFERENCES operation(id)
);

-- user: id (long), phone (nullable), username, password, email, active, audit (email, username y phone únicos)
CREATE TABLE "user" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT uq_user_phone UNIQUE (phone)
);

-- password_recovery_token: token de recuperación de contraseña (expira, un solo uso)
CREATE TABLE password_recovery_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_token_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT uq_recovery_token UNIQUE (token)
);

-- user_rol: N:N entre user y role, active, audit
CREATE TABLE user_rol (
    role_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (role_id, user_id),
    CONSTRAINT fk_user_rol_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_user_rol_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);
