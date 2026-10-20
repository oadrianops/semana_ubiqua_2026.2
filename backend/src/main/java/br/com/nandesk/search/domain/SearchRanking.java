package br.com.nandesk.search.domain;

import br.com.nandesk.catalog.domain.SearchNormalizer;
import java.util.Comparator;
import java.util.List;

/**
 * Pontuação da busca (9.10): igual = 100; começa com a consulta = 80; alguma palavra começa com a consulta = 60;
 * contém (ou contém todas as palavras da consulta) = 40; +10 se o termo é do idioma pedido. Desempate: mais boxes
 * com oferta disponível, depois nome em ordem alfabética.
 */
public final class SearchRanking {

    public static final int EXACT = 100;
    public static final int PREFIX = 80;
    public static final int WORD_PREFIX = 60;
    public static final int CONTAINS = 40;
    public static final int LOCALE_BONUS = 10;

    private SearchRanking() {}

    /** Pontua um termo já normalizado contra a consulta já normalizada. */
    public static int score(String normalizedTerm, String normalizedQuery, boolean sameLocale) {
        if (normalizedQuery.isEmpty()) {
            return 0;
        }
        int base = baseScore(normalizedTerm, normalizedQuery);
        return base == 0 ? 0 : base + (sameLocale ? LOCALE_BONUS : 0);
    }

    private static int baseScore(String term, String query) {
        if (term.equals(query)) {
            return EXACT;
        }
        if (term.startsWith(query)) {
            return PREFIX;
        }
        for (String word : term.split(" ")) {
            if (word.startsWith(query)) {
                return WORD_PREFIX;
            }
        }
        if (term.contains(query)) {
            return CONTAINS;
        }
        String[] queryTokens = query.split(" ");
        if (queryTokens.length > 1) {
            for (String token : queryTokens) {
                if (!term.contains(token)) {
                    return 0;
                }
            }
            return CONTAINS;
        }
        return 0;
    }

    public static List<Candidate> sort(List<Candidate> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingInt(Candidate::score)
                        .reversed()
                        .thenComparing(Comparator.comparingInt(Candidate::availableStalls)
                                .reversed())
                        .thenComparing(c -> SearchNormalizer.normalize(c.name())))
                .toList();
    }

    /** Produto candidato com a melhor pontuação entre seus termos. */
    public record Candidate(Long productId, String name, int score, int availableStalls) {}
}
