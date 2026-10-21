package br.com.nandesk.admin.application;

import br.com.nandesk.admin.application.SeedData.SeedMarket;
import br.com.nandesk.admin.application.SeedData.SeedOffering;
import br.com.nandesk.admin.application.SeedData.SeedProduct;
import br.com.nandesk.admin.application.SeedData.SeedStall;
import br.com.nandesk.admin.application.SeedData.SeedUser;
import br.com.nandesk.catalog.application.CatalogAdminService;
import br.com.nandesk.catalog.application.CatalogAdminService.CategoryCommand;
import br.com.nandesk.catalog.application.CatalogAdminService.ProductCommand;
import br.com.nandesk.catalog.application.CatalogAdminService.ProductTranslationCommand;
import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.ProductStatus;
import br.com.nandesk.commission.application.CommissionRuleService;
import br.com.nandesk.market.application.MarketAdminService;
import br.com.nandesk.market.application.MarketAdminService.MarketCommand;
import br.com.nandesk.market.application.MarketAdminService.SectorCommand;
import br.com.nandesk.market.application.MarketAdminService.Translation;
import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.application.StallAdminService.OfferingCommand;
import br.com.nandesk.stall.domain.Availability;
import br.com.nandesk.stall.domain.ConsentMethod;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.Role;
import br.com.nandesk.user.domain.User;
import br.com.nandesk.user.domain.UserStatus;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carga dos dados de demonstração (10.4): idempotente (procura por slug/código antes de inserir) e feita pelos
 * serviços de domínio. Senhas vêm de {@code SEED_DEFAULT_PASSWORD}. O teste do CA-1 usa o mesmo arquivo.
 */
@Service
public class DemoSeedService {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedService.class);
    public static final String SEED_FILE = "seed/dev-seed.json";

    private final JsonMapper json;
    private final UserAccountService users;
    private final MarketAdminService marketAdmin;
    private final MarketQueryService marketQuery;
    private final CatalogAdminService catalog;
    private final StallAdminService stallAdmin;
    private final CommissionRuleService commissionRules;
    private final AppProperties properties;
    private final Clock clock;

    public DemoSeedService(
            JsonMapper json,
            UserAccountService users,
            MarketAdminService marketAdmin,
            MarketQueryService marketQuery,
            CatalogAdminService catalog,
            StallAdminService stallAdmin,
            CommissionRuleService commissionRules,
            AppProperties properties,
            Clock clock) {
        this.json = json;
        this.users = users;
        this.marketAdmin = marketAdmin;
        this.marketQuery = marketQuery;
        this.catalog = catalog;
        this.stallAdmin = stallAdmin;
        this.commissionRules = commissionRules;
        this.properties = properties;
        this.clock = clock;
    }

    public SeedData read() {
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            return json.readValue(in, SeedData.class);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + SEED_FILE, e);
        }
    }

    @Transactional
    public void load() {
        SeedData data = read();
        String password = properties.seed().defaultPassword();
        if (password == null || password.length() < 8) {
            throw new IllegalStateException(
                    "Defina SEED_DEFAULT_PASSWORD (mínimo de 8 caracteres) para carregar o seed");
        }
        String adminPassword = properties.seed().adminPassword();
        if (adminPassword != null && !adminPassword.isBlank() && adminPassword.length() < 12) {
            throw new IllegalStateException("SEED_ADMIN_PASSWORD precisa de pelo menos 12 caracteres");
        }
        Map<String, Market> markets = new HashMap<>();
        for (SeedMarket m : data.markets()) {
            Market market = marketQuery.findActive(m.slug()).orElse(null);
            if (market == null) {
                Map<String, Translation> translations = new LinkedHashMap<>();
                m.translations()
                        .forEach((locale, t) ->
                                translations.put(locale, new Translation(t.description(), t.howToGetThere())));
                market = marketAdmin.upsert(new MarketCommand(
                        m.slug(),
                        m.name(),
                        m.address(),
                        m.latitude(),
                        m.longitude(),
                        m.radius(),
                        m.openingHours(),
                        true,
                        translations,
                        null));
                for (SeedData.SeedSector s : m.sectors()) {
                    marketAdmin.upsertSector(m.slug(), new SectorCommand(s.slug(), s.sort(), true, s.names()));
                }
            }
            markets.put(m.slug(), market);
        }

        User admin = null;
        for (SeedUser u : data.users()) {
            User user = users.findByEmail(u.email())
                    .orElseGet(() -> users.create(
                            u.name(),
                            u.email(),
                            properties.seed().passwordFor(u.role()),
                            Role.valueOf(u.role()),
                            UserStatus.ACTIVE,
                            "pt-BR"));
            if (!u.markets().isEmpty()) {
                users.replaceSubscriptions(
                        user.getId(),
                        u.markets().stream()
                                .map(slug -> markets.get(slug).getId())
                                .toList());
            }
            if (user.getRole() == Role.ADMIN) {
                admin = user;
            }
        }

        data.categories()
                .forEach(c ->
                        catalog.upsertCategory(new CategoryCommand(c.slug(), c.icon(), c.sort(), true, c.names())));

        Map<String, Product> products = new HashMap<>();
        for (SeedProduct p : data.products()) {
            Product product = catalog.upsertProduct(productCommand(p));
            products.put(p.slug(), product);
        }

        Instant now = clock.instant();
        for (SeedStall s : data.stalls()) {
            Market market = markets.get(s.market());
            if (stallAdmin.findByMarketAndCode(market, s.code()).isPresent()) {
                continue;
            }
            Sector sector = marketQuery.sector(market, s.sector()).orElse(null);
            Stall stall = stallAdmin.createStall(
                    market,
                    sector,
                    s.code(),
                    s.name(),
                    s.vendorName(),
                    Boolean.TRUE.equals(s.showVendorName()),
                    ConsentMethod.valueOf(s.consentMethod()),
                    admin);
            Instant verifiedAt = now.minus(Duration.ofDays(s.verifiedDaysAgo()));
            for (SeedOffering o : s.offerings()) {
                stallAdmin.putOffering(
                        stall,
                        new OfferingCommand(
                                products.get(o.product()),
                                o.price() == null ? null : new BigDecimal(o.price()),
                                PriceUnit.valueOf(o.unit()),
                                Availability.valueOf(o.availability())),
                        verifiedAt);
            }
            stallAdmin.markVerified(stall, verifiedAt);
        }

        if (data.commissionRules() != null && admin != null) {
            for (SeedData.SeedCommissionRule r : data.commissionRules()) {
                commissionRules.ensureRule(r.taskType(), new BigDecimal(r.amount()), admin);
            }
        }
        log.info(
                "Seed de demonstração carregado: {} feiras, {} produtos, {} boxes",
                markets.size(),
                products.size(),
                data.stalls().size());
    }

    private static ProductCommand productCommand(SeedProduct p) {
        Map<String, ProductTranslationCommand> translations = new LinkedHashMap<>();
        p.names()
                .forEach((locale, name) -> translations.put(
                        locale,
                        new ProductTranslationCommand(
                                name,
                                p.descriptions() == null
                                        ? null
                                        : p.descriptions().get(locale))));
        Map<String, List<String>> aliases = p.aliases() == null ? Map.of() : p.aliases();
        return new ProductCommand(
                p.slug(),
                p.category(),
                PriceUnit.valueOf(p.unit()),
                p.seasonMonths(),
                ProductStatus.ACTIVE,
                translations,
                aliases,
                null);
    }
}
