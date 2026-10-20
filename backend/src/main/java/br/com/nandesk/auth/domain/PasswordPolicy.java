package br.com.nandesk.auth.domain;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Política de senha (12.1, RNF-04): 8 a 128 caracteres e bloqueio das 10 mil senhas mais comuns; sem regras
 * forçadas de composição, conforme NIST SP 800-63B.
 */
public class PasswordPolicy {

    public static final int MIN = 8;
    public static final int MAX = 128;

    public enum Violation {
        TOO_SHORT,
        TOO_LONG,
        TOO_COMMON
    }

    private final Set<String> common;

    public PasswordPolicy(Set<String> common) {
        this.common = common;
    }

    public Set<Violation> check(String password) {
        Set<Violation> violations = EnumSet.noneOf(Violation.class);
        if (password == null || password.length() < MIN) {
            violations.add(Violation.TOO_SHORT);
        }
        if (password != null && password.length() > MAX) {
            violations.add(Violation.TOO_LONG);
        }
        if (password != null && common.contains(password.toLowerCase(Locale.ROOT))) {
            violations.add(Violation.TOO_COMMON);
        }
        return violations;
    }
}
