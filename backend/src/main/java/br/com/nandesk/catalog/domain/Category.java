package br.com.nandesk.catalog.domain;

import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.i18n.Translatable;
import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Agrupamento de produtos (ex.: Ervas e banhos de cheiro). */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    /** Nome do ícone do lucide-react exibido na interface. */
    @Column(name = "icon", nullable = false, length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryTranslation> translations = new LinkedHashSet<>();

    public Category(String slug, String icon, int sortOrder) {
        this.slug = slug;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    public void putName(String locale, String name) {
        translations.stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .ifPresentOrElse(
                        t -> t.setName(name), () -> translations.add(new CategoryTranslation(this, locale, name)));
    }

    public String name(AppLocale locale) {
        return Translatable.pick(translations, locale)
                .map(CategoryTranslation::getName)
                .orElse(slug);
    }
}
