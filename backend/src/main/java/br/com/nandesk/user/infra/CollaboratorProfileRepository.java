package br.com.nandesk.user.infra;

import br.com.nandesk.user.domain.CollaboratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollaboratorProfileRepository extends JpaRepository<CollaboratorProfile, Long> {}
