package br.com.nandesk.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Propriedades de negócio e de infraestrutura (especificação 16.5). Os padrões são os da especificação. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("America/Belem") @NotNull ZoneId timeZone,
        @DefaultValue("http://localhost:5173") @NotBlank String publicBaseUrl,
        @DefaultValue @Valid Cors cors,
        @DefaultValue @Valid Tasks tasks,
        @DefaultValue @Valid Stalls stalls,
        @DefaultValue @Valid Geo geo,
        @DefaultValue @Valid Photos photos,
        @DefaultValue @Valid Reputation reputation,
        @DefaultValue @Valid Payouts payouts,
        @DefaultValue @Valid Reports reports,
        @DefaultValue @Valid Notifications notifications,
        @DefaultValue @Valid Retention retention,
        @DefaultValue @Valid Uploads uploads,
        @DefaultValue @Valid Http http,
        @DefaultValue @Valid Snapshot snapshot,
        @DefaultValue @Valid Legal legal,
        @DefaultValue @Valid Jwt jwt,
        @DefaultValue @Valid Crypto crypto,
        @DefaultValue @Valid Auth auth,
        @DefaultValue @Valid Mail mail,
        @DefaultValue @Valid Push push,
        @DefaultValue @Valid Storage storage,
        @DefaultValue @Valid Seed seed) {

    public record Cors(@DefaultValue List<String> allowedOrigins) {}

    public record Tasks(
            @DefaultValue("2h") Duration checkinDeadline,
            @DefaultValue("1h") Duration submissionDeadline,
            @DefaultValue("15m") Duration expiryWarning,
            @DefaultValue("2") @Min(1) int maxActivePerCollaborator,
            @DefaultValue("15") @Min(1) int maxSubmissionsPerDay,
            @DefaultValue("30d") Duration staleAfter) {}

    public record Stalls(@DefaultValue("60d") Duration outdatedWarningAfter) {}

    public record Geo(
            @DefaultValue("250") @Min(1) int defaultRadiusMeters,
            @DefaultValue("100") @Min(0) int accuracyToleranceMeters) {}

    public record Photos(
            @DefaultValue("1") @Min(1) int minPerSubmission,
            @DefaultValue("5") @Min(1) int maxPerSubmission,
            @DefaultValue("5242880") long maxUploadSizeBytes,
            @DefaultValue("1600") int maxDimension,
            @DefaultValue("400") int thumbnailDimension,
            @DefaultValue("320") int minDimension,
            @DefaultValue("12000") int maxSourceDimension) {}

    public record Reputation(
            @DefaultValue("20") @Min(1) int window,
            @DefaultValue("80") @Min(0) int initialScore,
            @DefaultValue("40") @Min(0) int minToAccept,
            @DefaultValue("5") @Min(1) int minimumResults) {}

    public record Payouts(
            @DefaultValue("10.00") @DecimalMin("0.00") BigDecimal minimumAmount) {}

    public record Reports(
            @DefaultValue("3") @Min(1) int threshold,
            @DefaultValue("7d") Duration window,
            @DefaultValue("5") @Min(1) int maxPerHourPerIp) {}

    public record Notifications(
            @DefaultValue("10m") Duration pushCooldownPerMarket,
            @DefaultValue("22:00") LocalTime quietHoursStart,
            @DefaultValue("06:00") LocalTime quietHoursEnd,
            @DefaultValue("25s") Duration sseHeartbeat,
            @DefaultValue("60s") Duration streamTicketTtl) {}

    public record Retention(
            @DefaultValue("90d") Duration checkinCoordinates,
            @DefaultValue("90d") Duration evidencePhotos,
            @DefaultValue("30d") Duration rejectedPhotos,
            @DefaultValue("30d") Duration revokedStallPhotos,
            @DefaultValue("180d") Duration searchLogs,
            @DefaultValue("30d") Duration accountDeletion,
            @DefaultValue("90d") Duration readNotifications) {}

    public record Uploads(@DefaultValue("24h") Duration tempTtl) {}

    public record Http(
            @DefaultValue("public, max-age=60, stale-while-revalidate=600")
            String publicCacheControl) {}

    public record Snapshot(
            @DefaultValue("public, max-age=300, stale-while-revalidate=86400")
            String cacheControl) {}

    public record Legal(@DefaultValue("2026-10") String termsVersion) {}

    public record Jwt(
            @DefaultValue("nandesk") String issuer,
            @DefaultValue("nandesk-web") String audience,
            @DefaultValue("dev") String keyId,
            String privateKeyPath,
            String publicKeyPath,
            String privateKeyPem,
            String publicKeyPem,
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("7d") Duration refreshTokenTtl,
            @DefaultValue("30d") Duration refreshAbsoluteTtl) {}

    public record Crypto(String pixKey, @DefaultValue("1") int pixKeyVersion) {}

    public record Auth(
            @DefaultValue("nd_refresh") String refreshCookieName,
            @DefaultValue("false") boolean refreshCookieSecure,
            @DefaultValue("5") int lockoutThreshold,
            @DefaultValue("24h") Duration emailVerificationTtl,
            @DefaultValue("30m") Duration passwordResetTtl) {}

    /**
     * E-mail transacional. Sem SMTP configurado ({@code enabled=false}), as contas são verificadas
     * automaticamente para a demonstração não travar (decisão registrada no ADR-0017).
     */
    public record Mail(
            @DefaultValue("nao-responda@nandesk.com.br") String from,
            @DefaultValue("true") boolean enabled,
            @DefaultValue("contato@nandesk.com.br") String contact) {}

    public record Push(
            String vapidPublicKey,
            String vapidPrivateKey,
            @DefaultValue("mailto:contato@nandesk.com.br") String vapidSubject) {

        public boolean configured() {
            return vapidPublicKey != null
                    && !vapidPublicKey.isBlank()
                    && vapidPrivateKey != null
                    && !vapidPrivateKey.isBlank();
        }
    }

    public record Storage(@DefaultValue("./data/uploads") String localPath) {}

    public record Seed(@DefaultValue("false") boolean enabled, String defaultPassword) {}
}
