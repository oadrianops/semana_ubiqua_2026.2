-- V6 — crédito da foto de capa da feira (autor, licença e origem), exigido por fotos com licença livre como as do
-- Wikimedia Commons (RF-01, ADR-0019). Só acrescenta (expandir → contrair, seção 16.9).

ALTER TABLE markets
    ADD COLUMN cover_credit     VARCHAR(255) NULL AFTER cover_file_id,
    ADD COLUMN cover_credit_url VARCHAR(500) NULL AFTER cover_credit;
