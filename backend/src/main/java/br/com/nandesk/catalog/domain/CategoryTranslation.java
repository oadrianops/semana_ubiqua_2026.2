package br.com.nandesk.catalog.domain;

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
@Table(name = "category_translations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryTranslation extends BaseEntity implements Translatable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    CategoryTranslation(Category category, String locale, String name) {
        this.category = category;
        this.locale = locale;
        this.name = name;
    }
}
