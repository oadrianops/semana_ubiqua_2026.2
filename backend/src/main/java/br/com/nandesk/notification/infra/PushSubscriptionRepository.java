package br.com.nandesk.notification.infra;

import br.com.nandesk.notification.domain.PushSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findAllByUserId(Long userId);

    Optional<PushSubscription> findByEndpointHash(String endpointHash);

    Optional<PushSubscription> findByPublicIdAndUserId(UUID publicId, Long userId);

    void deleteAllByUserId(Long userId);
}
