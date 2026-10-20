package br.com.nandesk.user.infra;

import br.com.nandesk.user.domain.MarketSubscription;
import br.com.nandesk.user.domain.UserStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketSubscriptionRepository extends JpaRepository<MarketSubscription, Long> {

    List<MarketSubscription> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);

    /** Colaboradores ativos inscritos na feira (destinatários das notificações de tarefas — RF-14). */
    @Query("select s.user.id from MarketSubscription s where s.marketId = :marketId and s.user.status = :status")
    List<Long> findUserIdsByMarket(@Param("marketId") Long marketId, @Param("status") UserStatus status);
}
