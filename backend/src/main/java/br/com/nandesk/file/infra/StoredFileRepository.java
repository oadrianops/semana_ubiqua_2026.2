package br.com.nandesk.file.infra;

import br.com.nandesk.file.domain.FileStatus;
import br.com.nandesk.file.domain.StoredFile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByPublicId(UUID publicId);

    Optional<StoredFile> findByOwnerIdAndClientUploadId(Long ownerId, UUID clientUploadId);

    List<StoredFile> findAllByStatusAndDeleteAfterBefore(FileStatus status, Instant before);

    List<StoredFile> findAllByDeleteAfterBefore(Instant before);

    List<StoredFile> findAllByOriginalSha256AndIdNot(String sha256, Long id);
}
