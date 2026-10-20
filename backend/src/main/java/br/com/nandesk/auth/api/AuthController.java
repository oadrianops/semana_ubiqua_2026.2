package br.com.nandesk.auth.api;

import br.com.nandesk.auth.application.AuthService;
import br.com.nandesk.auth.application.AuthService.Session;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.application.UserAccountService.ProfileView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticação (RF-08 a RF-10, 12.1): token de acesso no corpo e token de renovação em cookie {@code HttpOnly},
 * {@code SameSite=Strict}, {@code Path=/api/v1/auth}. As rotas que usam o cookie checam o {@code Origin}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService auth;
    private final UserAccountService users;
    private final AppProperties properties;
    private final Clock clock;

    public AuthController(AuthService auth, UserAccountService users, AppProperties properties, Clock clock) {
        this.auth = auth;
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @Size(max = 10) String locale,
            @AssertTrue(message = "{validation.terms}") boolean acceptTerms,
            @AssertTrue(message = "{validation.adult}") boolean adultDeclaration) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 128) String password) {}

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {}

    public record EmailRequest(@NotBlank @Email String email) {}

    public record ResetRequest(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 8, max = 128) String password) {}

    public record SessionResponse(String accessToken, Instant expiresAt, ProfileView user) {}

    public record RegisterResponse(ProfileView user, boolean verificationRequired) {}

    @Operation(summary = "Cadastro de colaborador (RF-08)")
    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest body) {
        var user = auth.register(body.name(), body.email(), body.password(), body.locale());
        ProfileView view = users.profile(user.getPublicId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(view, !view.emailVerified()));
    }

    @Operation(summary = "Login (RF-09)")
    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(
            @Valid @RequestBody LoginRequest body,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return session(auth.login(body.email(), body.password(), userAgent));
    }

    @Operation(summary = "Renovar a sessão com o cookie (RF-09)")
    @PostMapping("/refresh")
    ResponseEntity<SessionResponse> refresh(
            HttpServletRequest request,
            @CookieValue(name = "${app.auth.refresh-cookie-name:nd_refresh}", required = false) String cookie,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        checkOrigin(request);
        try {
            return session(auth.refresh(cookie, userAgent));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                    .build();
        }
    }

    @Operation(summary = "Encerrar a sessão (RF-09)")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            HttpServletRequest request,
            @CookieValue(name = "${app.auth.refresh-cookie-name:nd_refresh}", required = false) String cookie) {
        checkOrigin(request);
        auth.logout(cookie);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                .build();
    }

    @Operation(summary = "Confirmar e-mail (RF-10)")
    @PostMapping("/verify-email")
    ResponseEntity<Void> verify(@Valid @RequestBody TokenRequest body) {
        auth.verifyEmail(body.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reenviar confirmação (RF-10)")
    @PostMapping("/verify-email/resend")
    ResponseEntity<Void> resend(@Valid @RequestBody EmailRequest body) {
        auth.resendVerification(body.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Esqueci a senha — resposta sempre 202 (RF-10)")
    @PostMapping("/password/forgot")
    ResponseEntity<Void> forgot(@Valid @RequestBody EmailRequest body) {
        auth.forgotPassword(body.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Redefinir a senha (RF-10)")
    @PostMapping("/password/reset")
    ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest body) {
        auth.resetPassword(body.token(), body.password());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<SessionResponse> session(Session session) {
        ResponseCookie cookie = ResponseCookie.from(properties.auth().refreshCookieName(), session.refreshToken())
                .httpOnly(true)
                .secure(properties.auth().refreshCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.between(clock.instant(), session.refreshExpiresAt()))
                .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new SessionResponse(
                        session.access().value(),
                        session.access().expiresAt(),
                        users.profile(session.user().getPublicId())));
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from(properties.auth().refreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.auth().refreshCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    /** Proteção extra além do SameSite=Strict: o Origin, quando presente, precisa ser o do app (12.1). */
    private void checkOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return;
        }
        List<String> allowed = new java.util.ArrayList<>(properties.cors().allowedOrigins());
        allowed.add(originOf(properties.publicBaseUrl()));
        String self = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? ""
                        : ":" + request.getServerPort());
        allowed.add(self);
        if (!allowed.contains(origin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static String originOf(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
    }
}
