package br.com.nandesk.shared.security;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.ProblemFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limites da tabela 12.3. O IP vem de {@code request.getRemoteAddr()}, que o Tomcat só troca pelo
 * {@code X-Forwarded-For} quando a requisição chega do proxy interno ({@code server.forward-headers-strategy}).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API = "/api/v1";
    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Duration HOUR = Duration.ofHours(1);

    private final RateLimiter limiter;
    private final ProblemFactory problems;
    private final JsonMapper jsonMapper;
    private final LocaleResolver localeResolver;
    private final AppProperties.RateLimits limits;

    public RateLimitFilter(
            RateLimiter limiter,
            ProblemFactory problems,
            JsonMapper jsonMapper,
            LocaleResolver localeResolver,
            AppProperties properties) {
        this.limits = properties.rateLimits();
        this.limiter = limiter;
        this.problems = problems;
        this.jsonMapper = jsonMapper;
        this.localeResolver = localeResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Rule> rule = ruleFor(request);
        if (rule.isPresent()) {
            long wait = limiter.tryConsume(
                    rule.get().key(), rule.get().capacity(), rule.get().period());
            if (wait > 0) {
                reject(request, response, wait);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Optional<Rule> ruleFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();
        Optional<CurrentUser> user = CurrentUser.find();
        if ("POST".equals(method)) {
            switch (path) {
                case API + "/auth/login":
                    return Optional.of(new Rule("login:" + ip, limits.loginPerMinute(), MINUTE));
                case API + "/auth/register":
                    return Optional.of(new Rule("register:" + ip, limits.registerPerHour(), HOUR));
                case API + "/auth/password/forgot":
                    return Optional.of(new Rule("forgot:" + ip, limits.forgotPerHour(), HOUR));
                case API + "/reports":
                    return Optional.of(new Rule("reports:" + ip, limits.reportsPerHour(), HOUR));
                case API + "/telemetry/web-vitals":
                    return Optional.of(new Rule("vitals:" + ip, limits.vitalsPerMinute(), MINUTE));
                case API + "/uploads":
                    return Optional.of(new Rule(
                            "uploads:" + user.map(u -> u.publicId().toString()).orElse(ip),
                            limits.uploadsPerMinute(),
                            MINUTE));
                default:
                    break;
            }
        }
        if ("GET".equals(method) && path.startsWith(API + "/search")) {
            return Optional.of(new Rule("search:" + ip, limits.searchPerMinute(), MINUTE));
        }
        return user.map(u -> new Rule("user:" + u.publicId(), limits.userPerMinute(), MINUTE));
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, long waitSeconds) throws IOException {
        LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
        try {
            var body = problems.create(
                    ErrorCode.RATE_LIMITED,
                    request.getRequestURI(),
                    Map.of("retryAfterSeconds", waitSeconds),
                    java.util.List.of());
            response.setStatus(ErrorCode.RATE_LIMITED.status().value());
            response.setHeader("Retry-After", Long.toString(waitSeconds));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            jsonMapper.writeValue(response.getOutputStream(), body);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    private record Rule(String key, int capacity, Duration period) {}
}
