package br.com.nandesk.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte exceções em Problem Details com código do catálogo 11.4; nunca vaza detalhes internos. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemFactory problems;

    public GlobalExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail body =
                problems.create(ex.code(), request.getRequestURI(), ex.extra(), List.of(), ex.messageArgs());
        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(ex.code().status()).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        Object retryAfter = ex.extra().get("retryAfterSeconds");
        if (retryAfter != null) {
            builder.header(HttpHeaders.RETRY_AFTER, retryAfter.toString());
        }
        return builder.body(body);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ProblemDetail> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return respond(ErrorCode.FORBIDDEN, request.getRequestURI());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleStale(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return respond(ErrorCode.STALE_VERSION, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<ProblemFactory.FieldErrorDto> errors = ex.getConstraintViolations().stream()
                .map(v -> new ProblemFactory.FieldErrorDto(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        ProblemDetail body = problems.create(ErrorCode.VALIDATION_ERROR, request.getRequestURI(), Map.of(), errors);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ProblemFactory.FieldErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toDto)
                .toList();
        ProblemDetail body = problems.create(ErrorCode.VALIDATION_ERROR, uri(request), Map.of(), errors);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return asObject(respond(ErrorCode.PAYLOAD_TOO_LARGE, uri(request)));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return asObject(respond(ErrorCode.NOT_FOUND, uri(request)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = switch (statusCode.value()) {
            case 401 -> ErrorCode.UNAUTHENTICATED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404, 405 -> ErrorCode.NOT_FOUND;
            case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
            default -> statusCode.is4xxClientError() ? ErrorCode.VALIDATION_ERROR : ErrorCode.INTERNAL_ERROR;
        };
        ProblemDetail problem = problems.create(code, uri(request));
        problem.setStatus(statusCode.value());
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorCode code, String instance) {
        return ResponseEntity.status(code.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problems.create(code, instance));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResponseEntity<Object> asObject(ResponseEntity<ProblemDetail> entity) {
        return (ResponseEntity) entity;
    }

    private static ProblemFactory.FieldErrorDto toDto(FieldError error) {
        return new ProblemFactory.FieldErrorDto(error.getField(), error.getDefaultMessage());
    }

    private static String uri(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
    }
}
