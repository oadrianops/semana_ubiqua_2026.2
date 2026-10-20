package br.com.nandesk.user.infra;

import br.com.nandesk.user.domain.Role;
import br.com.nandesk.user.domain.User;
import br.com.nandesk.user.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicId(UUID publicId);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    List<User> findAllByStatusAndDeletionRequestedAtBefore(UserStatus status, Instant before);

    List<User> findAllByStatus(UserStatus status);

    @Query("select u from User u where (:q is null or lower(u.name) like :q or lower(u.email) like :q)"
            + " and (:role is null or u.role = :role)")
    Page<User> search(@Param("q") String q, @Param("role") Role role, Pageable pageable);
}
