package br.com.nandesk.market.domain;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.i18n.Translatable;
import br.com.nandesk.shared.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Feira ou mercado público, com localização e raio de check-in (glossário, RN-05). */
@Entity
@Table(name = "markets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Market extends AuditableEntity {

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "geofence_radius_m", nullable = false)
    private int geofenceRadiusMeters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours")
    private List<OpeningHours> openingHours = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_file_id")
    private StoredFile cover;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** +1 a cada mudança publicada em setores, boxes ou ofertas (ETag do snapshot — RF-36). */
    @Column(name = "data_version", nullable = false)
    private long dataVersion;

    @OneToMany(mappedBy = "market", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MarketTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "market", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<Sector> sectors = new ArrayList<>();

    public Market(String slug, String name, String address, BigDecimal latitude, BigDecimal longitude, int radius) {
        this.slug = slug;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geofenceRadiusMeters = radius;
    }

    public void bumpDataVersion() {
        dataVersion++;
    }

    public void putTranslation(String locale, String description, String howToGetThere) {
        translations.stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .ifPresentOrElse(
                        t -> {
                            t.setDescription(description);
                            t.setHowToGetThere(howToGetThere);
                        },
                        () -> translations.add(new MarketTranslation(this, locale, description, howToGetThere)));
    }

    public MarketTranslation translation(AppLocale locale) {
        return Translatable.pick(translations, locale).orElse(null);
    }
}
