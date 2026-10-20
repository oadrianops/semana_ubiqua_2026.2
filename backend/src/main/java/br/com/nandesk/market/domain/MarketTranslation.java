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
@Table(name = "market_translations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketTranslation extends BaseEntity implements Translatable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "how_to_get_there", columnDefinition = "text")
    private String howToGetThere;

    MarketTranslation(Market market, String locale, String description, String howToGetThere) {
        this.market = market;
        this.locale = locale;
        this.description = description;
        this.howToGetThere = howToGetThere;
    }
}
