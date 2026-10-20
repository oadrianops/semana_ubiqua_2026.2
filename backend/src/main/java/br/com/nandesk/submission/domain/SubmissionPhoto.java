package br.com.nandesk.submission.domain;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Foto vinculada a um envio (RN-27): só do próprio dono e a um único envio. */
@Entity
@Table(name = "submission_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private PhotoRole role;

    @Column(name = "product_slug", length = 120)
    private String productSlug;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public SubmissionPhoto(Submission submission, StoredFile file, PhotoRole role, String productSlug, int sortOrder) {
        this.submission = submission;
        this.file = file;
        this.role = role;
        this.productSlug = productSlug;
        this.sortOrder = sortOrder;
    }
}
