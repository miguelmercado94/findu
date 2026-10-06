DROP TABLE IF EXISTS client_scopes;
DROP TABLE IF EXISTS client_post_logout_redirect_uris;
DROP TABLE IF EXISTS client_redirect_uris;
DROP TABLE IF EXISTS client_grant_types;
DROP TABLE IF EXISTS client_auth_methods;
DROP TABLE IF EXISTS frontend_client;

CREATE TABLE frontend_client (
    id VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_id_issued_at TIMESTAMP WITH TIME ZONE,
    client_secret VARCHAR(255),
    client_secret_expires_at TIMESTAMP WITH TIME ZONE,
    client_name VARCHAR(255),
    required_proof_key BOOLEAN NOT NULL DEFAULT FALSE,
    require_authorization_consent BOOLEAN NOT NULL DEFAULT FALSE,
    access_token_time_to_live_seconds BIGINT,
    refresh_token_time_to_live_seconds BIGINT,
    reuse_refresh_tokens BOOLEAN NOT NULL DEFAULT TRUE,
    duration_in_minutes INT NOT NULL DEFAULT 5
);

CREATE TABLE client_auth_methods (
    client_id VARCHAR(255) NOT NULL,
    method VARCHAR(255) NOT NULL,
    PRIMARY KEY (client_id, method),
    FOREIGN KEY (client_id) REFERENCES frontend_client(id) ON DELETE CASCADE
);

CREATE TABLE client_grant_types (
    client_id VARCHAR(255) NOT NULL,
    grant_type VARCHAR(255) NOT NULL,
    PRIMARY KEY (client_id, grant_type),
    FOREIGN KEY (client_id) REFERENCES frontend_client(id) ON DELETE CASCADE
);

CREATE TABLE client_redirect_uris (
    client_id VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(1000) NOT NULL,
    PRIMARY KEY (client_id, redirect_uri),
    FOREIGN KEY (client_id) REFERENCES frontend_client(id) ON DELETE CASCADE
);

CREATE TABLE client_post_logout_redirect_uris (
    client_id VARCHAR(255) NOT NULL,
    post_logout_redirect_uri VARCHAR(1000) NOT NULL,
    PRIMARY KEY (client_id, post_logout_redirect_uri),
    FOREIGN KEY (client_id) REFERENCES frontend_client(id) ON DELETE CASCADE
);

CREATE TABLE client_scopes (
    client_id VARCHAR(255) NOT NULL,
    scope VARCHAR(255) NOT NULL,
    PRIMARY KEY (client_id, scope),
    FOREIGN KEY (client_id) REFERENCES frontend_client(id) ON DELETE CASCADE
);
