package br.com.nandesk.admin.application;

import br.com.nandesk.market.domain.OpeningHours;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Estrutura de {@code seed/dev-seed.json} (especificação 10.4). */
public record SeedData(
        String notice,
        List<SeedUser> users,
        List<SeedMarket> markets,
        List<SeedCategory> categories,
        List<SeedProduct> products,
        List<SeedStall> stalls,
        List<SeedCommissionRule> commissionRules) {

    public record SeedUser(String email, String name, String role, List<String> markets) {}

    public record SeedTranslation(String description, String howToGetThere) {}

    public record SeedSector(String slug, int sort, Map<String, String> names) {}

    public record SeedMarket(
            String slug,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int radius,
            List<OpeningHours> openingHours,
            Map<String, SeedTranslation> translations,
            List<SeedSector> sectors) {}

    public record SeedCategory(String slug, String icon, int sort, Map<String, String> names) {}

    public record SeedProduct(
            String slug,
            String category,
            String unit,
            List<Integer> seasonMonths,
            Map<String, String> names,
            Map<String, String> descriptions,
            Map<String, List<String>> aliases) {}

    public record SeedOffering(String product, String price, String unit, String availability) {}

    public record SeedStall(
            String market,
            String sector,
            String code,
            String name,
            String vendorName,
            Boolean showVendorName,
            String consentMethod,
            int verifiedDaysAgo,
            List<SeedOffering> offerings,
            Boolean demo) {

        /** Box do seed é fictício salvo indicação contrária: aparece marcado como exemplo (ADR-0018). */
        public boolean isDemo() {
            return !Boolean.FALSE.equals(demo);
        }
    }

    public record SeedCommissionRule(String taskType, String amount) {}
}
