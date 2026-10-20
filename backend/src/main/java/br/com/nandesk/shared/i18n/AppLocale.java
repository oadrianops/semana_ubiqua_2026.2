package br.com.nandesk.shared.i18n;

import java.util.List;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

/** Idiomas suportados (P5): PT-BR (padrão e fallback — RN-19), inglês e espanhol. */
public enum AppLocale {
    PT_BR("pt-BR", Locale.forLanguageTag("pt-BR")),
    EN("en", Locale.ENGLISH),
    ES("es", Locale.forLanguageTag("es"));

    public static final List<Locale> SUPPORTED = List.of(PT_BR.locale, EN.locale, ES.locale);

    private final String tag;
    private final Locale locale;

    AppLocale(String tag, Locale locale) {
        this.tag = tag;
        this.locale = locale;
    }

    public String tag() {
        return tag;
    }

    public Locale locale() {
        return locale;
    }

    public static AppLocale fromTag(String tag) {
        if (tag == null) {
            return PT_BR;
        }
        String lang = tag.toLowerCase(Locale.ROOT);
        if (lang.startsWith("en")) {
            return EN;
        }
        if (lang.startsWith("es")) {
            return ES;
        }
        return PT_BR;
    }

    public static AppLocale from(Locale locale) {
        return locale == null ? PT_BR : fromTag(locale.toLanguageTag());
    }

    /** Idioma da requisição atual (resolvido a partir de {@code Accept-Language}). */
    public static AppLocale current() {
        return from(LocaleContextHolder.getLocale());
    }
}
