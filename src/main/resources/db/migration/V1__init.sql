CREATE TABLE users
(
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(30) NOT NULL UNIQUE,
    password            VARCHAR(80) NOT NULL,
    email               VARCHAR(50) UNIQUE,
    registration_status VARCHAR(30) NOT NULL,
    last_login          TIMESTAMP,
    deleted             BOOLEAN     NOT NULL DEFAULT false,
    created             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    INTEGER      NOT NULL,
    title      VARCHAR(255) NOT NULL UNIQUE,
    content    TEXT         NOT NULL,
    likes      INT          NOT NULL DEFAULT 0,
    deleted    BOOLEAN      NOT NULL DEFAULT false,
    created_by VARCHAR(50),
    created    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE roles
(
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(50) NOT NULL,
    user_system_role VARCHAR(64) NOT NULL,
    active           BOOLEAN     NOT NULL DEFAULT true,
    created_by       VARCHAR(50) NOT NULL
);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id INT    NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE refresh_token
(
    id      SERIAL PRIMARY KEY,
    token   VARCHAR(128) NOT NULL,
    created TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT       NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (user_id, id)
);

INSERT INTO users (username, password, email, registration_status, last_login, deleted, created, updated)
VALUES ('super_admin', '$2a$10$DsfW8SYQqh0QkOguDamm2Ot3BSCBIb/ETUrinnm01o1mAU/4.VA2y', 'super_admin@gmail.com', 'ACTIVE', CURRENT_TIMESTAMP, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('admin', '$2a$10$jPwJTuFH9s8Imlwujjo72evhq1pIjvEpMDf9jK4/hvTxjlOHvpgIa', 'admin@gmail.com','ACTIVE', CURRENT_TIMESTAMP, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('user', '$2a$10$1Widge9msbRvsuytuhm5PePuimq7iXz69g7aUgiyb/Wpgs8HP6xJC', 'user@gmail.com', 'ACTIVE', CURRENT_TIMESTAMP, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO posts(user_id, title, content, likes, deleted, created, updated)
VALUES (1, 'First Post', 'This is content of the first post', 6, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (1, 'Second Post', 'This is content of the second post', 3, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO roles (name, user_system_role, created_by)
VALUES ('SUPER_ADMIN', 'SUPER_ADMIN', 'SUPER_ADMIN'),
       ('ADMIN', 'ADMIN', 'SUPER_ADMIN'),
       ('USER', 'USER', 'SUPER_ADMIN');
INSERT INTO user_roles (user_id, role_id)
VALUES (1,1),
       (2,2),
       (3,3);
