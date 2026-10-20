package br.com.nandesk.market.domain;

import br.com.nandesk.shared.i18n.Translatable;
import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sector_translations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorTranslation extends BaseEntity implements Translatable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    SectorTranslation(Sector sector, String locale, String name) {
        this.sector = sector;
        this.locale = locale;
        this.name = name;
    }
}
