package br.com.nandesk.notification.infra;

import br.com.nandesk.notification.domain.Notification;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    java.util.Optional<Notification> findByPublicId(java.util.UUID publicId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null"
            + " and n.publicId in :ids")
    int markRead(@Param("userId") Long userId, @Param("ids") Collection<java.util.UUID> ids, @Param("now") Instant now);

    @Modifying
    @Query("delete from Notification n where n.readAt is not null and n.readAt < :before")
    int deleteReadBefore(@Param("before") Instant before);

    List<Notification> findAllByUserIdOrderByCreatedAtAsc(Long userId);
}
