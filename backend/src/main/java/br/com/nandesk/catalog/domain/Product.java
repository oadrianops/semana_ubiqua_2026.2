package br.com.nandesk.catalog.domain;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.i18n.Translatable;
import br.com.nandesk.shared.persistence.AuditableEntity;
import br.com.nandesk.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Item canônico do catálogo, com traduções e sinônimos (ex.: castanha-do-pará). */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditableEntity {

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_unit", nullable = false, length = 20)
    private PriceUnit defaultUnit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "season_months")
    private List<Integer> seasonMonths;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id")
    private StoredFile image;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private Product mergedInto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_by")
    private User suggestedBy;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductTranslation> translations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductAlias> aliases = new LinkedHashSet<>();

    public Product(String slug, Category category, PriceUnit defaultUnit, ProductStatus status) {
        this.slug = slug;
        this.category = category;
        this.defaultUnit = defaultUnit;
        this.status = status;
    }

    public void putTranslation(String locale, String name, String description) {
        translations.stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .ifPresentOrElse(
                        t -> {
                            t.setName(name);
                            t.setDescription(description);
                        },
                        () -> translations.add(new ProductTranslation(this, locale, name, description)));
    }

    /**
     * Sinônimo por idioma, sem repetir: a comparação ignora acentos e maiúsculas, como a collation
     * utf8mb4_0900_ai_ci do índice único ("açaí" e "acai" são o mesmo sinônimo).
     */
    public void addAlias(String locale, String alias) {
        String key = SearchNormalizer.normalize(alias);
        boolean exists = aliases.stream()
                .anyMatch(a -> a.getLocale().equals(locale)
                        && SearchNormalizer.normalize(a.getAlias()).equals(key));
        if (!exists) {
            aliases.add(new ProductAlias(this, locale, alias));
        }
    }

    public Optional<ProductTranslation> translation(AppLocale locale) {
        return Translatable.pick(translations, locale);
    }

    public String name(AppLocale locale) {
        return translation(locale).map(ProductTranslation::getName).orElse(slug);
    }

    public String description(AppLocale locale) {
        return translation(locale).map(ProductTranslation::getDescription).orElse(null);
    }

    public boolean isPublic() {
        return status == ProductStatus.ACTIVE;
    }
}
