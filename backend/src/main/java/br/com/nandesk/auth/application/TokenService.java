package br.com.nandesk.auth.application;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Token de acesso JWT RS256 de 15 min com {@code sub} = publicId e {@code roles} (12.1). */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final AppProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, AppProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public record AccessToken(String value, Instant expiresAt) {}

    public AccessToken issue(User user) {
        Instant now = clock.instant();
        Instant exp = now.plus(properties.jwt().accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .audience(List.of(properties.jwt().audience()))
                .subject(user.getPublicId().toString())
                .issuedAt(now)
                .expiresAt(exp)
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.jwt().keyId())
                .build();
        return new AccessToken(
                encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), exp);
    }
}
