package br.com.nandesk.auth.infra;

import br.com.nandesk.auth.domain.OneTimeToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<OneTimeToken> findByTokenHashAndPurpose(String tokenHash, OneTimeToken.Purpose purpose);

    @Modifying
    @Query("delete from OneTimeToken t where t.expiresAt < :before")
    int deleteExpired(@Param("before") Instant before);
}
