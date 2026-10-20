-- V4 — comissões, extrato, repasses, orçamento, denúncias, notificações, push e auditoria
-- (RF-07, RF-14, RF-25 a RF-28, RF-33; RN-07 a RN-10, RN-16).

CREATE TABLE commission_rules (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    task_type  VARCHAR(20)    NOT NULL,
    amount     DECIMAL(10, 2) NOT NULL,
    valid_from DATETIME(6)    NOT NULL,
    valid_to   DATETIME(6)    NULL,
    created_by BIGINT         NULL,
    created_at DATETIME(6)    NOT NULL,
    CONSTRAINT pk_commission_rules PRIMARY KEY (id),
    CONSTRAINT ck_commission_rules_amount CHECK (amount >= 0),
    CONSTRAINT fk_commission_rules_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_commission_rules_type_valid_from (task_type, valid_from)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE payouts (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    public_id          CHAR(36)       NOT NULL,
    collaborator_id    BIGINT         NOT NULL,
    amount             DECIMAL(10, 2) NOT NULL,
    method             VARCHAR(20)    NOT NULL,
    external_reference VARCHAR(120)   NULL,
    paid_at            DATETIME(6)    NOT NULL,
    registered_by      BIGINT         NOT NULL,
    notes              VARCHAR(255)   NULL,
    created_at         DATETIME(6)    NOT NULL,
    CONSTRAINT pk_payouts PRIMARY KEY (id),
    CONSTRAINT uk_payouts_public_id UNIQUE (public_id),
    CONSTRAINT ck_payouts_amount CHECK (amount > 0),
    CONSTRAINT fk_payouts_collaborator FOREIGN KEY (collaborator_id) REFERENCES users (id),
    CONSTRAINT fk_payouts_registered_by FOREIGN KEY (registered_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Somente INSERT (RN-08): a aplicação nunca altera nem apaga lançamentos.
CREATE TABLE ledger_entries (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    public_id         CHAR(36)       NOT NULL,
    collaborator_id   BIGINT         NOT NULL,
    entry_type        VARCHAR(20)    NOT NULL,
    amount            DECIMAL(10, 2) NOT NULL,
    task_id           BIGINT         NULL,
    payout_id         BIGINT         NULL,
    reverses_entry_id BIGINT         NULL,
    description       VARCHAR(255)   NOT NULL,
    created_by        BIGINT         NULL,
    created_at        DATETIME(6)    NOT NULL,
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT uk_ledger_entries_public_id UNIQUE (public_id),
    CONSTRAINT fk_ledger_entries_collaborator FOREIGN KEY (collaborator_id) REFERENCES users (id),
    CONSTRAINT fk_ledger_entries_task FOREIGN KEY (task_id) REFERENCES update_tasks (id),
    CONSTRAINT fk_ledger_entries_payout FOREIGN KEY (payout_id) REFERENCES payouts (id),
    CONSTRAINT fk_ledger_entries_reverses FOREIGN KEY (reverses_entry_id) REFERENCES ledger_entries (id),
    CONSTRAINT fk_ledger_entries_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_ledger_entries_collaborator_created (collaborator_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE program_budgets (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    period       CHAR(7)        NOT NULL,
    sponsor_name VARCHAR(120)   NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    created_by   BIGINT         NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    CONSTRAINT pk_program_budgets PRIMARY KEY (id),
    CONSTRAINT uk_program_budgets_period_sponsor UNIQUE (period, sponsor_name),
    CONSTRAINT ck_program_budgets_amount CHECK (amount >= 0),
    CONSTRAINT fk_program_budgets_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        CHAR(36)     NOT NULL,
    stall_id         BIGINT       NOT NULL,
    product_id       BIGINT       NULL,
    reason           VARCHAR(30)  NOT NULL,
    comment          VARCHAR(500) NULL,
    reporter_user_id BIGINT       NULL,
    status           VARCHAR(20)  NOT NULL,
    task_id          BIGINT       NULL,
    created_at       DATETIME(6)  NOT NULL,
    resolved_at      DATETIME(6)  NULL,
    CONSTRAINT pk_reports PRIMARY KEY (id),
    CONSTRAINT uk_reports_public_id UNIQUE (public_id),
    CONSTRAINT fk_reports_stall FOREIGN KEY (stall_id) REFERENCES stalls (id),
    CONSTRAINT fk_reports_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id),
    CONSTRAINT fk_reports_task FOREIGN KEY (task_id) REFERENCES update_tasks (id),
    INDEX idx_reports_stall_created (stall_id, created_at),
    INDEX idx_reports_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    public_id   CHAR(36)     NOT NULL,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(40)  NOT NULL,
    message_key VARCHAR(80)  NOT NULL,
    params      JSON         NULL,
    link        VARCHAR(255) NULL,
    read_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uk_notifications_public_id UNIQUE (public_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_notifications_user_read_created (user_id, read_at, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE push_subscriptions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    public_id       CHAR(36)     NOT NULL,
    user_id         BIGINT       NOT NULL,
    endpoint        TEXT         NOT NULL,
    endpoint_hash   CHAR(64)     NOT NULL,
    p256dh          VARCHAR(255) NOT NULL,
    auth            VARCHAR(255) NOT NULL,
    user_agent      VARCHAR(255) NULL,
    last_success_at DATETIME(6)  NULL,
    failure_count   INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_push_subscriptions PRIMARY KEY (id),
    CONSTRAINT uk_push_subscriptions_public_id UNIQUE (public_id),
    CONSTRAINT uk_push_subscriptions_endpoint_hash UNIQUE (endpoint_hash),
    CONSTRAINT fk_push_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT      NULL,
    action        VARCHAR(60) NOT NULL,
    entity_type   VARCHAR(40) NOT NULL,
    entity_id     VARCHAR(36) NOT NULL,
    before_data   JSON        NULL,
    after_data    JSON        NULL,
    request_id    VARCHAR(36) NULL,
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    INDEX idx_audit_log_entity (entity_type, entity_id),
    INDEX idx_audit_log_actor_created (actor_user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
