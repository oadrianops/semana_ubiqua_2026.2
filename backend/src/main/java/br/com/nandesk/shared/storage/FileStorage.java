package br.com.nandesk.shared.storage;

import java.io.InputStream;
import java.util.Optional;

/** Armazenamento de arquivos atrás de uma interface (ADR-0009): local hoje, S3-compatível no futuro. */
public interface FileStorage {

    void store(String key, byte[] content);

    Optional<InputStream> load(String key);

    void delete(String key);
}
