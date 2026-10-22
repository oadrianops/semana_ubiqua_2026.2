package br.com.nandesk.stall.domain;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Foto da galeria do box (fachada e produtos), pública enquanto houver consentimento (RF-03, RN-13). */
@Entity
@Table(name = "stall_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StallPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stall_id", nullable = false)
    private Stall stall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Setter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StallPhoto(Stall stall, StoredFile file, int sortOrder, Instant createdAt) {
        this.stall = stall;
        this.file = file;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }
}
