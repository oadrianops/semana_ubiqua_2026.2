package br.com.nandesk.catalog.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalização da busca (9.10): trim → minúsculas → sem acentos (NFD sem marcas) → hífens e pontuação viram
 * espaço → espaços repetidos viram um. A versão do frontend segue os mesmos casos de
 * {@code contracts/search-normalization-cases.json}.
 */
public final class SearchNormalizer {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SearchNormalizer() {}

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String lower = input.trim().toLowerCase(Locale.ROOT);
        String noMarks =
                MARKS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALNUM.matcher(noMarks).replaceAll(" ").trim();
    }

    public static List<String> tokens(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(normalized.split(" "));
    }
}
