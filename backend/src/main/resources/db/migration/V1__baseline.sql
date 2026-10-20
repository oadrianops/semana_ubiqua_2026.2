-- V1 — baseline: contas, arquivos, feiras, setores, catálogo, boxes, ofertas e registros de busca.
-- Especificação 10.1 (convenções) e 10.3 (dicionário). Datas em UTC; dinheiro DECIMAL(10,2); enums VARCHAR.

CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    public_id             CHAR(36)     NOT NULL,
    name                  VARCHAR(120) NOT NULL,
    email                 VARCHAR(254) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    phone                 VARCHAR(20)  NULL,
    role                  VARCHAR(20)  NOT NULL,
    status                VARCHAR(30)  NOT NULL,
    preferred_locale      VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    email_verified_at     DATETIME(6)  NULL,
    terms_version         VARCHAR(20)  NOT NULL,
    terms_accepted_at     DATETIME(6)  NOT NULL,
    adult_declared_at     DATETIME(6)  NOT NULL,
    deletion_requested_at DATETIME(6)  NULL,
    last_login_at         DATETIME(6)  NULL,
    failed_login_count    INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME(6)  NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    version               INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_public_id UNIQUE (public_id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE stored_files (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        CHAR(36)     NOT NULL,
    storage_key      VARCHAR(255) NOT NULL,
    thumbnail_key    VARCHAR(255) NOT NULL,
    content_type     VARCHAR(50)  NOT NULL,
    size_bytes       INT          NOT NULL,
    width            INT          NOT NULL,
    height           INT          NOT NULL,
    original_sha256  CHAR(64)     NOT NULL,
    visibility       VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    client_upload_id CHAR(36)     NULL,
    owner_user_id    BIGINT       NULL,
    delete_after     DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_stored_files PRIMARY KEY (id),
    CONSTRAINT uk_stored_files_public_id UNIQUE (public_id),
    CONSTRAINT uk_stored_files_storage_key UNIQUE (storage_key),
    CONSTRAINT uk_stored_files_owner_client_upload UNIQUE (owner_user_id, client_upload_id),
    CONSTRAINT fk_stored_files_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    INDEX idx_stored_files_original_sha256 (original_sha256),
    INDEX idx_stored_files_status_delete_after (status, delete_after)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE markets (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    public_id         CHAR(36)      NOT NULL,
    slug              VARCHAR(80)   NOT NULL,
    name              VARCHAR(120)  NOT NULL,
    address           VARCHAR(255)  NOT NULL,
    latitude          DECIMAL(9, 6) NOT NULL,
    longitude         DECIMAL(9, 6) NOT NULL,
    geofence_radius_m INT           NOT NULL DEFAULT 250,
    opening_hours     JSON          NULL,
    cover_file_id     BIGINT        NULL,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    data_version      BIGINT        NOT NULL DEFAULT 0,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    version           INT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_markets PRIMARY KEY (id),
    CONSTRAINT uk_markets_public_id UNIQUE (public_id),
    CONSTRAINT uk_markets_slug UNIQUE (slug),
    CONSTRAINT fk_markets_cover_file FOREIGN KEY (cover_file_id) REFERENCES stored_files (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE market_translations (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    market_id        BIGINT      NOT NULL,
    locale           VARCHAR(10) NOT NULL,
    description      TEXT        NOT NULL,
    how_to_get_there TEXT        NULL,
    CONSTRAINT pk_market_translations PRIMARY KEY (id),
    CONSTRAINT uk_market_translations_market_locale UNIQUE (market_id, locale),
    CONSTRAINT fk_market_translations_market FOREIGN KEY (market_id) REFERENCES markets (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE sectors (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    public_id  CHAR(36)    NOT NULL,
    market_id  BIGINT      NOT NULL,
    slug       VARCHAR(80) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_sectors PRIMARY KEY (id),
    CONSTRAINT uk_sectors_public_id UNIQUE (public_id),
    CONSTRAINT uk_sectors_market_slug UNIQUE (market_id, slug),
    CONSTRAINT fk_sectors_market FOREIGN KEY (market_id) REFERENCES markets (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE sector_translations (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    sector_id BIGINT       NOT NULL,
    locale    VARCHAR(10)  NOT NULL,
    name      VARCHAR(120) NOT NULL,
    CONSTRAINT pk_sector_translations PRIMARY KEY (id),
    CONSTRAINT uk_sector_translations_sector_locale UNIQUE (sector_id, locale),
    CONSTRAINT fk_sector_translations_sector FOREIGN KEY (sector_id) REFERENCES sectors (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    public_id  CHAR(36)    NOT NULL,
    slug       VARCHAR(80) NOT NULL,
    icon       VARCHAR(50) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_public_id UNIQUE (public_id),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE category_translations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    category_id BIGINT       NOT NULL,
    locale      VARCHAR(10)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    CONSTRAINT pk_category_translations PRIMARY KEY (id),
    CONSTRAINT uk_category_translations_category_locale UNIQUE (category_id, locale),
    CONSTRAINT fk_category_translations_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    public_id      CHAR(36)     NOT NULL,
    slug           VARCHAR(120) NOT NULL,
    category_id    BIGINT       NOT NULL,
    default_unit   VARCHAR(20)  NOT NULL,
    season_months  JSON         NULL,
    image_file_id  BIGINT       NULL,
    status         VARCHAR(20)  NOT NULL,
    merged_into_id BIGINT       NULL,
    suggested_by   BIGINT       NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    version        INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_public_id UNIQUE (public_id),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_image_file FOREIGN KEY (image_file_id) REFERENCES stored_files (id),
    CONSTRAINT fk_products_merged_into FOREIGN KEY (merged_into_id) REFERENCES products (id),
    CONSTRAINT fk_products_suggested_by FOREIGN KEY (suggested_by) REFERENCES users (id),
    INDEX idx_products_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product_translations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    locale      VARCHAR(10)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description TEXT         NULL,
    CONSTRAINT pk_product_translations PRIMARY KEY (id),
    CONSTRAINT uk_product_translations_product_locale UNIQUE (product_id, locale),
    CONSTRAINT fk_product_translations_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product_aliases (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    product_id BIGINT       NOT NULL,
    locale     VARCHAR(10)  NOT NULL,
    alias      VARCHAR(120) NOT NULL,
    CONSTRAINT pk_product_aliases PRIMARY KEY (id),
    CONSTRAINT uk_product_aliases_product_locale_alias UNIQUE (product_id, locale, alias),
    CONSTRAINT fk_product_aliases_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product_search_terms (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    product_id      BIGINT       NOT NULL,
    locale          VARCHAR(10)  NOT NULL,
    term            VARCHAR(120) NOT NULL,
    normalized_term VARCHAR(120) NOT NULL,
    source          VARCHAR(10)  NOT NULL,
    CONSTRAINT pk_product_search_terms PRIMARY KEY (id),
    CONSTRAINT uk_product_search_terms_product_locale_term UNIQUE (product_id, locale, normalized_term),
    CONSTRAINT fk_product_search_terms_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_product_search_terms_normalized_term (normalized_term)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE stalls (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    public_id           CHAR(36)      NOT NULL,
    market_id           BIGINT        NOT NULL,
    sector_id           BIGINT        NULL,
    code                VARCHAR(20)   NOT NULL,
    display_name        VARCHAR(120)  NULL,
    vendor_name         VARCHAR(120)  NULL,
    show_vendor_name    BOOLEAN       NOT NULL DEFAULT FALSE,
    photo_file_id       BIGINT        NULL,
    latitude            DECIMAL(9, 6) NULL,
    longitude           DECIMAL(9, 6) NULL,
    status              VARCHAR(20)   NOT NULL,
    consent_status      VARCHAR(20)   NOT NULL,
    consent_method      VARCHAR(20)   NOT NULL,
    consent_recorded_at DATETIME(6)   NOT NULL,
    consent_recorded_by BIGINT        NOT NULL,
    consent_revoked_at  DATETIME(6)   NULL,
    last_verified_at    DATETIME(6)   NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    version             INT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_stalls PRIMARY KEY (id),
    CONSTRAINT uk_stalls_public_id UNIQUE (public_id),
    CONSTRAINT uk_stalls_market_code UNIQUE (market_id, code),
    CONSTRAINT fk_stalls_market FOREIGN KEY (market_id) REFERENCES markets (id),
    CONSTRAINT fk_stalls_sector FOREIGN KEY (sector_id) REFERENCES sectors (id),
    CONSTRAINT fk_stalls_photo_file FOREIGN KEY (photo_file_id) REFERENCES stored_files (id),
    CONSTRAINT fk_stalls_consent_recorded_by FOREIGN KEY (consent_recorded_by) REFERENCES users (id),
    INDEX idx_stalls_market_status_consent (market_id, status, consent_status),
    INDEX idx_stalls_last_verified_at (last_verified_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE stall_offerings (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    public_id        CHAR(36)       NOT NULL,
    stall_id         BIGINT         NOT NULL,
    product_id       BIGINT         NOT NULL,
    reference_price  DECIMAL(10, 2) NULL,
    price_unit       VARCHAR(20)    NULL,
    availability     VARCHAR(20)    NOT NULL,
    last_verified_at DATETIME(6)    NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    version          INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_stall_offerings PRIMARY KEY (id),
    CONSTRAINT uk_stall_offerings_public_id UNIQUE (public_id),
    CONSTRAINT uk_stall_offerings_stall_product UNIQUE (stall_id, product_id),
    CONSTRAINT fk_stall_offerings_stall FOREIGN KEY (stall_id) REFERENCES stalls (id),
    CONSTRAINT fk_stall_offerings_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_stall_offerings_product_availability (product_id, availability)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE search_logs (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    query_normalized VARCHAR(120) NOT NULL,
    locale           VARCHAR(10)  NOT NULL,
    results_count    INT          NOT NULL,
    market_filter    VARCHAR(80)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_search_logs PRIMARY KEY (id),
    INDEX idx_search_logs_created_at (created_at),
    INDEX idx_search_logs_results_query (results_count, query_normalized)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
