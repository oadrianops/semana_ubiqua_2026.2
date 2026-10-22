-- V5 — galeria de fotos do box, Instagram do box e marca de box fictício da demonstração
-- (RF-03, RF-29, RN-13, ADR-0018). Só acrescenta (expandir → contrair, seção 16.9).

ALTER TABLE stalls
    ADD COLUMN demo             BOOLEAN     NOT NULL DEFAULT FALSE AFTER last_verified_at,
    ADD COLUMN instagram_handle VARCHAR(30) NULL AFTER demo;

CREATE TABLE stall_photos (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    stall_id   BIGINT      NOT NULL,
    file_id    BIGINT      NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_stall_photos PRIMARY KEY (id),
    CONSTRAINT uk_stall_photos_file UNIQUE (file_id),
    CONSTRAINT fk_stall_photos_stall FOREIGN KEY (stall_id) REFERENCES stalls (id),
    CONSTRAINT fk_stall_photos_file FOREIGN KEY (file_id) REFERENCES stored_files (id),
    INDEX idx_stall_photos_stall_sort (stall_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
