package br.com.nandesk.stall.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Perfil do Instagram de um box (RF-03): aceita "@perfil", "perfil" ou o link do perfil e guarda só o nome, em
 * minúsculas. Nomes do Instagram têm até 30 letras, números, pontos e sublinhados, sem ponto no início, no fim ou
 * repetido.
 */
public final class InstagramHandle {

    private static final Pattern VALID = Pattern.compile("^(?!\\.)(?!.*\\.\\.)[a-z0-9._]{1,30}(?<!\\.)$");
    private static final Pattern LINK =
            Pattern.compile("^(?:https?://)?(?:www\\.)?(?:instagram\\.com|instagr\\.am)/", Pattern.CASE_INSENSITIVE);

    private InstagramHandle() {}

    /** Vazio quando não há perfil; {@link IllegalArgumentException} quando o texto não é um perfil válido. */
    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = LINK.matcher(raw.trim()).replaceFirst("");
        int cut = indexOfAny(value, '/', '?', '#');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        if (value.startsWith("@")) {
            value = value.substring(1);
        }
        value = value.toLowerCase(Locale.ROOT);
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Perfil do Instagram inválido");
        }
        return Optional.of(value);
    }

    public static String profileUrl(String handle) {
        return handle == null ? null : "https://www.instagram.com/" + handle + "/";
    }

    private static int indexOfAny(String s, char... chars) {
        int best = -1;
        for (char c : chars) {
            int i = s.indexOf(c);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }
}
