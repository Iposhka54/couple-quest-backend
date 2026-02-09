--liquibase formatted sql

--changeset auth-service:create-schema
CREATE SCHEMA IF NOT EXISTS auth_service;

--changeset auth-service:create-users-and-gender-table
CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(64) UNIQUE,
    password   VARCHAR(128) NOT NULL,
    name       VARCHAR(32)  NOT NULL,
    gender     SMALLINT     NOT NULL,
    created_at TIMESTAMP    DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE couple
(
    id            BIGSERIAL PRIMARY KEY,
    boy_id        BIGINT REFERENCES users (id),
    girlfriend_id BIGINT REFERENCES users (id),
    status        SMALLINT  NOT NULL,
    created_at    TIMESTAMP DEFAULT now()
);

CREATE TABLE couple_invite
(
    id              UUID PRIMARY KEY,
    couple_id       BIGINT REFERENCES couple (id),
    inviter_id      BIGINT REFERENCES users (id),
    token_hash      VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMP,
    expected_gender SMALLINT     NOT NULL,
    status          SMALLINT     NOT NULL,
    created_at      TIMESTAMP    DEFAULT now()
);