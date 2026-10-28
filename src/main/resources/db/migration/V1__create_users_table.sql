CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       full_name     VARCHAR(100) NOT NULL,
                       email         VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);