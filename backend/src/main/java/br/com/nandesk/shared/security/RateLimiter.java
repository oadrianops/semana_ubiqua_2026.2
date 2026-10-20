package br.com.nandesk.shared.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Limitação de taxa em memória (Bucket4j + Caffeine — 12.3). Com várias réplicas, trocar por um armazenamento
 * compartilhado (RNF-14).
 */
@Component
public class RateLimiter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(200_000)
            .build();

    /**
     * Consome uma ficha do balde identificado por {@code key}.
     *
     * @return 0 se permitido; caso contrário, os segundos até a próxima ficha.
     */
    public long tryConsume(String key, int capacity, Duration period) {
        Bucket bucket = buckets.get(
                key + "|" + capacity + "|" + period.toSeconds(),
                k -> Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(capacity, period)
                                .build())
                        .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return 0;
        }
        return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    }

    /** Limpa os baldes (usado nos testes). */
    public void reset() {
        buckets.invalidateAll();
    }
}
