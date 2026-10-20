package br.com.nandesk.market.domain;

import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.i18n.Translatable;
import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Área temática dentro da feira (ex.: setor das ervas). */
@Entity
@Table(name = "sectors")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sector extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SectorTranslation> translations = new ArrayList<>();

    public Sector(Market market, String slug, int sortOrder) {
        this.market = market;
        this.slug = slug;
        this.sortOrder = sortOrder;
    }

    public void putName(String locale, String name) {
        translations.stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .ifPresentOrElse(
                        t -> t.setName(name), () -> translations.add(new SectorTranslation(this, locale, name)));
    }

    public String name(AppLocale locale) {
        return Translatable.pick(translations, locale)
                .map(SectorTranslation::getName)
                .orElse(slug);
    }
}
