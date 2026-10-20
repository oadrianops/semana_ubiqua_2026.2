package br.com.nandesk.shared.security;

import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.ProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

/** Respostas 401 e 403 da camada de segurança no mesmo formato Problem Details da API (11.4). */
@Component
public class ProblemAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemFactory problems;
    private final JsonMapper jsonMapper;
    private final LocaleResolver localeResolver;

    public ProblemAuthHandlers(ProblemFactory problems, JsonMapper jsonMapper, LocaleResolver localeResolver) {
        this.problems = problems;
        this.jsonMapper = jsonMapper;
        this.localeResolver = localeResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        write(request, response, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(request, response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
        try {
            ProblemDetail body = problems.create(code, request.getRequestURI());
            response.setStatus(code.status().value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            if (code == ErrorCode.UNAUTHENTICATED) {
                response.setHeader("WWW-Authenticate", "Bearer");
            }
            jsonMapper.writeValue(response.getOutputStream(), body);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
