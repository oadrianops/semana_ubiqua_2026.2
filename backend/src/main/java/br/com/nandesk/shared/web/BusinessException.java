package br.com.nandesk.shared.web;

import java.util.Map;

/** Exceção de negócio com código estável do catálogo 11.4; convertida em Problem Details. */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final transient Object[] messageArgs;
    private final transient Map<String, Object> extra;

    public BusinessException(ErrorCode code, Object... messageArgs) {
        this(code, Map.of(), messageArgs);
    }

    public BusinessException(ErrorCode code, Map<String, Object> extra, Object... messageArgs) {
        super(code.name());
        this.code = code;
        this.messageArgs = messageArgs;
        this.extra = extra;
    }

    public static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND);
    }

    public ErrorCode code() {
        return code;
    }

    public Object[] messageArgs() {
        return messageArgs;
    }

    /** Campos extras do Problem Details (ex.: {@code retryAfterSeconds}). */
    public Map<String, Object> extra() {
        return extra;
    }
}
