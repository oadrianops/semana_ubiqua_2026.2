package br.com.nandesk.notification.domain;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Endereços aceitos para Web Push (12.7, SSRF): o servidor só faz POST para os serviços de push dos navegadores,
 * nunca para um endereço qualquer informado pelo cliente (ex.: a rede interna do Docker).
 */
public final class PushEndpointPolicy {

    /** FCM (Chrome/Android), Mozilla, Windows e Apple; sempre HTTPS. */
    public static final List<String> DEFAULT_PATTERNS = List.of(
            "^https://fcm\\.googleapis\\.com/",
            "^https://([a-z0-9-]+\\.)*push\\.services\\.mozilla\\.com/",
            "^https://([a-z0-9-]+\\.)*notify\\.windows\\.com/",
            "^https://web\\.push\\.apple\\.com/",
            "^https://([a-z0-9-]+\\.)*push\\.apple\\.com/");

    private final List<Pattern> patterns;

    public PushEndpointPolicy(List<String> patterns) {
        List<String> source = patterns == null || patterns.isEmpty() ? DEFAULT_PATTERNS : patterns;
        this.patterns = source.stream().map(Pattern::compile).toList();
    }

    public boolean allows(String endpoint) {
        if (endpoint == null || endpoint.length() > 2048) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        String normalized = uri.getScheme().toLowerCase() + "://"
                + uri.getHost().toLowerCase() + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + (uri.getRawPath() == null ? "" : uri.getRawPath());
        return patterns.stream().anyMatch(p -> p.matcher(normalized).find());
    }
}
