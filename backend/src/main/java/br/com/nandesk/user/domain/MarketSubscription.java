package br.com.nandesk.user.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Feira de interesse do colaborador: define quem recebe as notificações de tarefas (RF-11, RF-14). A feira é
 * referenciada pelo id para o módulo de usuários não depender do módulo de feiras (ADR-0003).
 */
@Entity
@Table(name = "collaborator_market_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MarketSubscription(User user, Long marketId) {
        this.user = user;
        this.marketId = marketId;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }
}
