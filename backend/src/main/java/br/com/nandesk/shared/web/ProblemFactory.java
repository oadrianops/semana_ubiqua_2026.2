package br.com.nandesk.shared.web;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/** Monta Problem Details (RFC 9457) com {@code code}, {@code requestId} e textos traduzidos (11.1, 11.4). */
@Component
public class ProblemFactory {

    private final MessageSource messages;

    public ProblemFactory(MessageSource messages) {
        this.messages = messages;
    }

    public ProblemDetail create(ErrorCode code, String instance, Object... args) {
        return create(code, instance, Map.of(), List.of(), args);
    }

    public ProblemDetail create(
            ErrorCode code,
            String instance,
            Map<String, Object> extra,
            List<FieldErrorDto> fieldErrors,
            Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        ProblemDetail problem = ProblemDetail.forStatus(code.status());
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(messages.getMessage(code.titleKey(), null, code.name(), locale));
        problem.setDetail(messages.getMessage(code.detailKey(), args, code.name(), locale));
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        }
        problem.setProperty("code", code.name());
        String requestId = RequestIdFilter.current();
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("errors", fieldErrors);
        }
        extra.forEach(problem::setProperty);
        return problem;
    }

    /** Erro de um campo na lista {@code errors} do Problem Details. */
    public record FieldErrorDto(String field, String message) {}
}
