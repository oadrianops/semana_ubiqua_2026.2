package br.com.nandesk.shared.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

/**
 * ETag e Cache-Control para os GET públicos (9.14): a revalidação com {@code If-None-Match} responde 304
 * sem montar o corpo quando a versão é conhecida antes (snapshot) ou sem reenviar o corpo (demais rotas).
 */
public final class HttpCaching {

    private HttpCaching() {}

    /** ETag forte derivado de uma versão conhecida; o corpo só é montado se a versão mudou. */
    public static <T> ResponseEntity<T> versioned(
            WebRequest request, String version, String cacheControl, String language, Supplier<T> body) {
        String etag = "\"" + version + "\"";
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                    .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                    .build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CONTENT_LANGUAGE, language)
                .body(body.get());
    }

    /** Cabeçalho padrão das rotas públicas; o ETag fica a cargo do {@code ShallowEtagHeaderFilter}. */
    public static <T> ResponseEntity<T> publicResponse(String cacheControl, String language, T body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CONTENT_LANGUAGE, language)
                .body(body);
    }

    public static CacheControl noStore() {
        return CacheControl.noStore();
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
