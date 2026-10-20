package br.com.nandesk.shared.security;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Identidade de quem fez a requisição, lida do JWT ({@code sub} = publicId do usuário — 12.1). */
public record CurrentUser(UUID publicId, Set<String> roles) {

    public static Optional<CurrentUser> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
            Set<String> roles = jwt.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet());
            return Optional.of(new CurrentUser(UUID.fromString(jwt.getToken().getSubject()), roles));
        }
        return Optional.empty();
    }

    public static CurrentUser require() {
        return find().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }

    public boolean isModerator() {
        return roles.contains("ROLE_MODERATOR") || roles.contains("ROLE_ADMIN");
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
