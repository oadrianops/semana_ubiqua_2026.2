package br.com.nandesk.file.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import br.com.nandesk.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Metadados de uma imagem armazenada (seções 9.9 e 10.3). */
@Entity
@Table(name = "stored_files")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "thumbnail_key", nullable = false, length = 255)
    private String thumbnailKey;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "original_sha256", nullable = false, length = 64)
    private String originalSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private FileVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "client_upload_id", length = 36)
    private UUID clientUploadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(name = "delete_after")
    private Instant deleteAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StoredFile(
            String storageKey,
            String thumbnailKey,
            int sizeBytes,
            int width,
            int height,
            String originalSha256,
            FileVisibility visibility,
            FileStatus status) {
        this.storageKey = storageKey;
        this.thumbnailKey = thumbnailKey;
        this.contentType = "image/jpeg";
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.originalSha256 = originalSha256;
        this.visibility = visibility;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = ClockHolder.now();
        }
    }

    /** URL pública relativa usada pelos DTOs. */
    public String url(boolean thumbnail) {
        return "/api/v1/files/" + publicId + (thumbnail ? "?variant=thumb" : "");
    }
}
