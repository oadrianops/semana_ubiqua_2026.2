package br.com.nandesk.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Caches em memória (Caffeine) com TTL por finalidade (seções 9.10, 9.13 e 9.14). */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    public static final String SEARCH = "search";
    public static final String SUGGESTIONS = "suggestions";
    public static final String SNAPSHOT = "snapshot";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(
                SEARCH,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(60))
                        .maximumSize(5_000)
                        .recordStats()
                        .build());
        manager.registerCustomCache(
                SUGGESTIONS,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(60))
                        .maximumSize(5_000)
                        .build());
        manager.registerCustomCache(
                SNAPSHOT,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(30))
                        .maximumSize(60)
                        .recordStats()
                        .build());
        return manager;
    }
}
