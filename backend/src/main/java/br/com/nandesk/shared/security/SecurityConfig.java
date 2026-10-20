package br.com.nandesk.shared.security;

import br.com.nandesk.shared.config.AppProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * API stateless com JWT próprio (RS256) e hierarquia ADMIN &gt; MODERATOR &gt; COLLABORATOR (seções 4 e 12).
 * As regras por prefixo seguem a tabela 11.2; checagens de dono ficam nos serviços.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            ProblemAuthHandlers handlers,
            RateLimitFilter rateLimitFilter,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .oauth2ResourceServer(o -> o.authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers)
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .authorizeHttpRequests(auth -> auth
                        // Público (11.2)
                        .requestMatchers(
                                HttpMethod.GET,
                                API + "/markets/**",
                                API + "/stalls/**",
                                API + "/search/**",
                                API + "/search",
                                API + "/categories/**",
                                API + "/products/**",
                                API + "/files/**",
                                API + "/config/public")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, API + "/reports", API + "/telemetry/web-vitals")
                        .permitAll()
                        .requestMatchers(API + "/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, API + "/me/notifications/stream")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                        .permitAll()
                        // Administração restrita a ADMIN (11.2)
                        .requestMatchers(
                                API + "/admin/commission-rules/**", API + "/admin/balances/**",
                                API + "/admin/collaborators/**", API + "/admin/payouts/**",
                                API + "/admin/ledger-adjustments/**", API + "/admin/budgets/**",
                                API + "/admin/users/**", API + "/admin/audit-log/**")
                        .hasRole("ADMIN")
                        .requestMatchers(API + "/admin/**", API + "/moderation/**")
                        .hasRole("MODERATOR")
                        .requestMatchers(API + "/tasks/**", API + "/uploads/**", API + "/catalog/**")
                        .hasRole("COLLABORATOR")
                        .requestMatchers(API + "/me/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll())
                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /** O filtro roda só dentro da cadeia de segurança (depois da autenticação), não como filtro do servlet. */
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MODERATOR
                ROLE_MODERATOR > ROLE_COLLABORATOR
                """);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtKeys keys) {
        RSAKey rsa = new RSAKey.Builder(keys.publicKey())
                .privateKey(keys.privateKey())
                .keyID(keys.keyId())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtKeys keys, AppProperties properties) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        String audience = properties.jwt().audience();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()), audienceValidator));
        return decoder;
    }

    /** BCrypt (custo 12) via DelegatingPasswordEncoder (12.1). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = properties.cors().allowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setExposedHeaders(List.of("ETag", "X-Request-Id", "Retry-After", "Content-Language"));
            config.setAllowCredentials(true);
            source.registerCorsConfiguration("/api/**", config);
        }
        return source;
    }
}
