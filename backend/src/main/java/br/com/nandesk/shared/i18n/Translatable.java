package br.com.nandesk.shared.i18n;

import java.util.Collection;
import java.util.Optional;

/** Tradução de uma entidade em um idioma ({@code <entidade>_translations} — 10.1). */
public interface Translatable {

    String getLocale();

    /** Escolhe a tradução do idioma pedido, com fallback para PT-BR (RN-19). */
    static <T extends Translatable> Optional<T> pick(Collection<T> translations, AppLocale locale) {
        Optional<T> exact = translations.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(locale.tag()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return translations.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(AppLocale.PT_BR.tag()))
                .findFirst()
                .or(() -> translations.stream().findFirst());
    }
}
