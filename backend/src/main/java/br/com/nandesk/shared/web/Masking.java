package br.com.nandesk.shared.web;

/** Mascaramento para logs e exibição (seções 9.3 e 13.4). */
public final class Masking {

    private Masking() {}

    /** {@code ana.silva@dominio.com} → {@code a***@d***.com}. */
    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String domain = parts[1];
        int dot = domain.lastIndexOf('.');
        String tld = dot >= 0 ? domain.substring(dot) : "";
        return first(parts[0]) + "***@" + first(domain) + "***" + tld;
    }

    /** Chave PIX mascarada, mostrando só o fim (ex.: {@code ••••-4f2a}). */
    public static String pixKey(String key) {
        if (key == null || key.length() < 4) {
            return "••••";
        }
        return "••••" + key.substring(key.length() - 4);
    }

    private static String first(String value) {
        return value.isEmpty() ? "" : value.substring(0, 1);
    }
}
