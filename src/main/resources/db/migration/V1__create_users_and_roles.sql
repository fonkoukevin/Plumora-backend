CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    id_role UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(30) UNIQUE NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE users (
    id_user UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firstname VARCHAR(80) NOT NULL,
    lastname VARCHAR(80) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    bio TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE user_roles (
    id_user UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    id_role UUID NOT NULL REFERENCES roles(id_role) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user, id_role)
);

INSERT INTO roles (name, description) VALUES
    ('AUTHOR', 'Can write, manage and publish books'),
    ('READER', 'Can read published books and interact with the catalog'),
    ('BETA_READER', 'Can participate in beta-reading campaigns'),
    ('ADMIN', 'Can administer Plumora')
ON CONFLICT (name) DO NOTHING;
