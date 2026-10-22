package br.com.nandesk.search.application;

import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.catalog.application.CatalogViews.TermHit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.SearchNormalizer;
import br.com.nandesk.market.application.MarketViews.MarketRef;
import br.com.nandesk.search.application.SearchViews.MarketHits;
import br.com.nandesk.search.application.SearchViews.SearchItem;
import br.com.nandesk.search.application.SearchViews.SearchResult;
import br.com.nandesk.search.application.SearchViews.StallHit;
import br.com.nandesk.search.application.SearchViews.Suggestion;
import br.com.nandesk.search.domain.SearchRanking;
import br.com.nandesk.search.domain.SearchRanking.Candidate;
import br.com.nandesk.shared.config.CacheConfig;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallQueryService;
import br.com.nandesk.stall.domain.Availability;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.stall.domain.StallOffering;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca de produtos (RF-02, seção 9.10): termos normalizados no MySQL, ranqueamento em Java e resultado
 * agrupado por produto → feira → boxes. Só entram boxes ativos e com consentimento (RN-13).
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final double DID_YOU_MEAN_THRESHOLD = 0.88;
    private static final int SUGGESTIONS_LIMIT = 8;

    private final CatalogQueryService catalog;
    private final StallQueryService stalls;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public SearchService(CatalogQueryService catalog, StallQueryService stalls) {
        this.catalog = catalog;
        this.stalls = stalls;
    }

    public record Query(
            String text, String marketSlug, String categorySlug, boolean availableOnly, int page, int size) {}

    @Cacheable(
            cacheNames = CacheConfig.SEARCH,
            key = "T(br.com.nandesk.catalog.domain.SearchNormalizer).normalize(#query.text()) + '|' + #locale"
                    + " + '|' + #query.marketSlug() + '|' + #query.categorySlug() + '|' + #query.availableOnly()"
                    + " + '|' + #query.page() + '|' + #query.size()")
    public SearchResult search(Query query, AppLocale locale) {
        String normalized = SearchNormalizer.normalize(query.text());
        int size = PageResponse.clampSize(query.size());
        int page = Math.max(0, query.page());
        if (normalized.isEmpty()) {
            return new SearchResult(query.text(), locale.tag(), List.of(), page, size, 0, 0, List.of());
        }
        Map<Long, Integer> scores = scoreProducts(normalized, locale);
        Map<Long, Product> products = catalog.productsById(scores.keySet());
        if (query.categorySlug() != null) {
            products.values().removeIf(p -> !p.getCategory().getSlug().equals(query.categorySlug()));
        }
        List<StallOffering> offerings =
                stalls.publicOfferingsFor(products.keySet(), query.marketSlug(), query.availableOnly());
        Map<Long, List<StallOffering>> byProduct = new HashMap<>();
        offerings.forEach(o -> byProduct
                .computeIfAbsent(o.getProduct().getId(), k -> new ArrayList<>())
                .add(o));

        List<Candidate> ranked = SearchRanking.sort(byProduct.keySet().stream()
                .map(id -> new Candidate(
                        id, products.get(id).name(locale), scores.get(id), (int) byProduct.get(id).stream()
                                .filter(o -> o.getAvailability() == Availability.AVAILABLE)
                                .count()))
                .toList());
        List<SearchItem> items = ranked.stream()
                .skip((long) page * size)
                .limit(size)
                .map(c -> item(products.get(c.productId()), byProduct.get(c.productId()), locale))
                .toList();
        List<String> didYouMean = ranked.isEmpty() ? didYouMean(normalized) : List.of();
        long total = ranked.size();
        return new SearchResult(
                query.text(),
                locale.tag(),
                items,
                page,
                size,
                total,
                (int) Math.ceil((double) total / size),
                didYouMean);
    }

    /** Sugestões enquanto digita (a partir de 2 caracteres, até 8). */
    @Cacheable(
            cacheNames = CacheConfig.SUGGESTIONS,
            key = "T(br.com.nandesk.catalog.domain.SearchNormalizer).normalize(#text) + '|' + #locale")
    public List<Suggestion> suggestions(String text, AppLocale locale) {
        String normalized = SearchNormalizer.normalize(text);
        if (normalized.length() < 2) {
            return List.of();
        }
        Map<Long, Integer> scores = scoreProducts(normalized, locale);
        Map<Long, Product> products = catalog.productsById(scores.keySet());
        return scores.entrySet().stream()
                .filter(e -> products.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Integer>comparingByValue()
                        .reversed()
                        .thenComparing(e -> SearchNormalizer.normalize(
                                products.get(e.getKey()).name(locale))))
                .limit(SUGGESTIONS_LIMIT)
                .map(e -> {
                    Product p = products.get(e.getKey());
                    return new Suggestion(
                            p.getSlug(), p.name(locale), p.getCategory().getSlug());
                })
                .toList();
    }

    /** Melhor pontuação de cada produto entre seus termos (nomes e sinônimos nos três idiomas). */
    private Map<Long, Integer> scoreProducts(String normalized, AppLocale locale) {
        List<String> tokens = SearchNormalizer.tokens(normalized);
        String longest =
                tokens.stream().max(Comparator.comparingInt(String::length)).orElse(normalized);
        Map<Long, Integer> scores = new HashMap<>();
        for (TermHit hit : catalog.termCandidates(normalized, longest)) {
            boolean sameLocale = hit.locale().equalsIgnoreCase(locale.tag());
            int score = SearchRanking.score(hit.normalizedTerm(), normalized, sameLocale);
            if (score > 0) {
                scores.merge(hit.productId(), score, Math::max);
            }
        }
        return scores;
    }

    private SearchItem item(Product product, List<StallOffering> offerings, AppLocale locale) {
        Map<String, List<StallOffering>> byMarket = new LinkedHashMap<>();
        offerings.stream()
                .sorted(Comparator.comparing(
                                (StallOffering o) -> o.getStall().getMarket().getName())
                        .thenComparing(StallOffering::getLastVerifiedAt, Comparator.reverseOrder()))
                .forEach(o -> byMarket.computeIfAbsent(o.getStall().getMarket().getSlug(), k -> new ArrayList<>())
                        .add(o));
        List<MarketHits> markets = byMarket.values().stream()
                .map(list -> new MarketHits(
                        new MarketRef(
                                list.getFirst().getStall().getMarket().getSlug(),
                                list.getFirst().getStall().getMarket().getName()),
                        list.stream().map(o -> hit(o, locale)).toList()))
                .toList();
        return new SearchItem(CatalogQueryService.ref(product, locale), markets);
    }

    static StallHit hit(StallOffering o, AppLocale locale) {
        Stall s = o.getStall();
        return new StallHit(
                s.getPublicId(),
                s.getCode(),
                s.getDisplayName(),
                s.getSector() == null ? null : s.getSector().name(locale),
                MoneyDto.brl(o.getReferencePrice()),
                o.getPriceUnit(),
                o.getAvailability(),
                o.getLastVerifiedAt(),
                s.isDemo());
    }

    /** "Você quis dizer…?" (Could): até 3 termos com Jaro-Winkler ≥ 0,88. */
    private List<String> didYouMean(String normalized) {
        return catalog.allActiveTerms().stream()
                .map(TermHit::normalizedTerm)
                .filter(Objects::nonNull)
                .distinct()
                .map(term -> Map.entry(term, similarity.apply(term, normalized)))
                .filter(e -> e.getValue() >= DID_YOU_MEAN_THRESHOLD)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }
}
