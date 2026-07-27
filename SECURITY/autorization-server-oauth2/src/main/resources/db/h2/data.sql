-- 1. CLIENTE: App móvil 'findu-cliente' (PKCE Obligatorio)
INSERT INTO frontend_client (id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, required_proof_key, require_authorization_consent, access_token_time_to_live_seconds, refresh_token_time_to_live_seconds, reuse_refresh_tokens, duration_in_minutes)
VALUES ('findu-cliente', 'findu-cliente', CURRENT_TIMESTAMP, '$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO', NULL, 'FindU Cliente Mobile App', TRUE, TRUE, 600, 2400, TRUE, 10);

-- 2. PROVEEDOR: App móvil 'findu-proveedor' (PKCE Obligatorio)
INSERT INTO frontend_client (id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, required_proof_key, require_authorization_consent, access_token_time_to_live_seconds, refresh_token_time_to_live_seconds, reuse_refresh_tokens, duration_in_minutes)
VALUES ('findu-proveedor', 'findu-proveedor', CURRENT_TIMESTAMP, '$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO', NULL, 'FindU Proveedor Mobile App', TRUE, TRUE, 600, 2400, TRUE, 10);

-- 3. ADMIN: App web 'findu-admin' (Cliente Confidencial)
INSERT INTO frontend_client (id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, required_proof_key, require_authorization_consent, access_token_time_to_live_seconds, refresh_token_time_to_live_seconds, reuse_refresh_tokens, duration_in_minutes)
VALUES ('findu-admin', 'findu-admin', CURRENT_TIMESTAMP, '$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO', NULL, 'FindU Admin Web App', FALSE, TRUE, 600, 2400, TRUE, 10);


-- Métodos de Autenticación
INSERT INTO client_auth_methods (client_id, method) VALUES ('findu-cliente', 'client_secret_basic');
INSERT INTO client_auth_methods (client_id, method) VALUES ('findu-cliente', 'client_secret_post');

INSERT INTO client_auth_methods (client_id, method) VALUES ('findu-proveedor', 'client_secret_basic');
INSERT INTO client_auth_methods (client_id, method) VALUES ('findu-proveedor', 'client_secret_post');

INSERT INTO client_auth_methods (client_id, method) VALUES ('findu-admin', 'client_secret_basic');


-- Tipos de Flujo de Autorización (Grant Types)
INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-cliente', 'authorization_code');
INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-cliente', 'refresh_token');

INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-proveedor', 'authorization_code');
INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-proveedor', 'refresh_token');

INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-admin', 'authorization_code');
INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-admin', 'refresh_token');
INSERT INTO client_grant_types (client_id, grant_type) VALUES ('findu-admin', 'client_credentials');


-- URIs de Redirección (Redirect URIs)
INSERT INTO client_redirect_uris (client_id, redirect_uri) VALUES ('findu-cliente', 'https://oauthdebugger.com/debug');
INSERT INTO client_redirect_uris (client_id, redirect_uri) VALUES ('findu-cliente', 'findu-cliente://callback');

INSERT INTO client_redirect_uris (client_id, redirect_uri) VALUES ('findu-proveedor', 'https://oauthdebugger.com/debug');
INSERT INTO client_redirect_uris (client_id, redirect_uri) VALUES ('findu-proveedor', 'findu-proveedor://callback');

INSERT INTO client_redirect_uris (client_id, redirect_uri) VALUES ('findu-admin', 'https://oauthdebugger.com/debug');
INSERT INTO client_redirect_uris (client_id, redirect_uri) VALUES ('findu-admin', 'http://localhost:3000/api/auth/callback/findu-admin');


-- Permisos y Alcances (Scopes) - Mismos scopes estándar para todos
-- Scopes para 'findu-cliente'
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-cliente', 'openid');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-cliente', 'profile');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-cliente', 'read');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-cliente', 'write');

-- Scopes para 'findu-proveedor'
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-proveedor', 'openid');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-proveedor', 'profile');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-proveedor', 'read');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-proveedor', 'write');

-- Scopes para 'findu-admin'
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-admin', 'openid');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-admin', 'profile');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-admin', 'read');
INSERT INTO client_scopes (client_id, scope) VALUES ('findu-admin', 'write');
