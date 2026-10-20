-- V2 — perfis de colaborador, feiras de interesse, tokens de renovação e de uso único (RF-08 a RF-11; 12.1).

CREATE TABLE collaborator_profiles (
    user_id           BIGINT        NOT NULL,
    pix_key_type      VARCHAR(20)   NULL,
    pix_key_encrypted VARBINARY(512) NULL,
    pix_key_masked    VARCHAR(60)   NULL,
    reputation_score  INT           NOT NULL DEFAULT 80,
    push_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at        DATETIME(6)   NOT NULL,
    CONSTRAINT pk_collaborator_profiles PRIMARY KEY (user_id),
    CONSTRAINT fk_collaborator_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE collaborator_market_subscriptions (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    market_id  BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_collaborator_market_subscriptions PRIMARY KEY (id),
    CONSTRAINT uk_collaborator_market_subscriptions_user_market UNIQUE (user_id, market_id),
    CONSTRAINT fk_collaborator_market_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_collaborator_market_subscriptions_market FOREIGN KEY (market_id) REFERENCES markets (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    family_id      CHAR(36)     NOT NULL,
    token_hash     CHAR(64)     NOT NULL,
    expires_at     DATETIME(6)  NOT NULL,
    absolute_expires_at DATETIME(6) NOT NULL,
    revoked_at     DATETIME(6)  NULL,
    replaced_by_id BIGINT       NULL,
    user_agent     VARCHAR(255) NULL,
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id),
    INDEX idx_refresh_tokens_user_revoked (user_id, revoked_at),
    INDEX idx_refresh_tokens_family (family_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE one_time_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    purpose    VARCHAR(30) NOT NULL,
    token_hash CHAR(64)    NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_one_time_tokens PRIMARY KEY (id),
    CONSTRAINT uk_one_time_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_one_time_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
