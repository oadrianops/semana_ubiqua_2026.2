package br.com.nandesk.file.application;

import br.com.nandesk.file.domain.FileStatus;
import br.com.nandesk.file.domain.FileVisibility;
import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.file.infra.StoredFileRepository;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.storage.FileStorage;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.user.domain.User;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Armazenamento e entrega de imagens (seção 9.9) e uploads temporários idempotentes (RN-27). */
@Service
public class FileService {

    private final StoredFileRepository files;
    private final FileStorage storage;
    private final ImagePipeline pipeline;
    private final AppProperties properties;
    private final Clock clock;

    public FileService(StoredFileRepository files, FileStorage storage, AppProperties properties, Clock clock) {
        this.files = files;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
        AppProperties.Photos p = properties.photos();
        this.pipeline = new ImagePipeline(
                p.maxUploadSizeBytes(),
                p.maxDimension(),
                p.thumbnailDimension(),
                p.minDimension(),
                p.maxSourceDimension());
    }

    public record Upload(UUID fileId, FileStatus status, int width, int height, Instant expiresAt, boolean created) {}

    public record Content(InputStream stream, String contentType, boolean publicCache) {}

    /**
     * Upload de uma foto com a chave do aparelho (RN-27): repetir a mesma chave devolve o mesmo arquivo.
     * A foto fica {@code TEMP} até ser vinculada a um envio; vence em {@code app.uploads.temp-ttl}.
     */
    @Transactional
    public Upload uploadTemp(User owner, UUID clientUploadId, byte[] bytes) {
        Optional<StoredFile> existing = files.findByOwnerIdAndClientUploadId(owner.getId(), clientUploadId);
        if (existing.isPresent()) {
            return toUpload(existing.get(), false);
        }
        StoredFile file = storeProcessed("submissions", bytes, FileVisibility.RESTRICTED, FileStatus.TEMP);
        file.setOwner(owner);
        file.setClientUploadId(clientUploadId);
        file.setDeleteAfter(clock.instant().plus(properties.uploads().tempTtl()));
        return toUpload(files.save(file), true);
    }

    /** Grava uma imagem pública já vinculada (seed e painel administrativo). */
    @Transactional
    public StoredFile storePublic(String area, byte[] bytes) {
        return files.save(storeProcessed(area, bytes, FileVisibility.PUBLIC, FileStatus.ATTACHED));
    }

    /** Fotos de evidência só para o dono e para moderadores; públicas para todos (9.9). */
    @Transactional(readOnly = true)
    public Content open(UUID publicId, boolean thumbnail) {
        StoredFile file = files.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
        boolean isPublic = file.getVisibility() == FileVisibility.PUBLIC;
        if (!isPublic) {
            CurrentUser user = CurrentUser.find().orElseThrow(BusinessException::notFound);
            boolean owner =
                    file.getOwner() != null && file.getOwner().getPublicId().equals(user.publicId());
            if (!owner && !user.isModerator()) {
                throw BusinessException.notFound();
            }
        }
        String key = thumbnail ? file.getThumbnailKey() : file.getStorageKey();
        InputStream stream = storage.load(key).orElseThrow(BusinessException::notFound);
        return new Content(stream, file.getContentType(), isPublic);
    }

    @Transactional(readOnly = true)
    public Optional<StoredFile> find(UUID publicId) {
        return files.findByPublicId(publicId);
    }

    /** Fotos com o mesmo hash usadas em outro arquivo (alerta de foto repetida na moderação — RF-21). */
    @Transactional(readOnly = true)
    public List<StoredFile> duplicatesOf(StoredFile file) {
        return files.findAllByOriginalSha256AndIdNot(file.getOriginalSha256(), file.getId());
    }

    /** Apaga do disco e do banco (retenção e órfãos — RN-22, RN-27). */
    @Transactional
    public void purge(StoredFile file) {
        files.delete(file);
        files.flush();
        storage.delete(file.getStorageKey());
        storage.delete(file.getThumbnailKey());
    }

    /** Fotos anexadas cujo prazo de retenção venceu (RN-13, RN-22): os donos desfazem o vínculo antes da remoção. */
    @Transactional(readOnly = true)
    public List<Long> attachedDueForRetention() {
        return files.findAllByDeleteAfterBefore(clock.instant()).stream()
                .filter(f -> f.getStatus() == FileStatus.ATTACHED)
                .map(StoredFile::getId)
                .toList();
    }

    @Transactional
    public int purgeExpiredTemp() {
        List<StoredFile> expired = files.findAllByStatusAndDeleteAfterBefore(FileStatus.TEMP, clock.instant());
        expired.forEach(this::purge);
        return expired.size();
    }

    @Transactional
    public int purgeAfterRetention() {
        List<StoredFile> expired = files.findAllByDeleteAfterBefore(clock.instant()).stream()
                .filter(f -> f.getStatus() == FileStatus.ATTACHED)
                .toList();
        for (StoredFile f : expired) {
            purge(f);
        }
        return expired.size();
    }

    private StoredFile storeProcessed(String area, byte[] bytes, FileVisibility visibility, FileStatus status) {
        ImagePipeline.Processed processed = pipeline.process(bytes);
        ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
        String base = "%s/%04d/%02d/%s".formatted(area, now.getYear(), now.getMonthValue(), UUID.randomUUID());
        String key = base + ".jpg";
        String thumbKey = base + "_thumb.jpg";
        storage.store(key, processed.jpeg());
        storage.store(thumbKey, processed.thumbnail());
        return new StoredFile(
                key,
                thumbKey,
                processed.jpeg().length,
                processed.width(),
                processed.height(),
                processed.sha256(),
                visibility,
                status);
    }

    private static Upload toUpload(StoredFile f, boolean created) {
        return new Upload(f.getPublicId(), f.getStatus(), f.getWidth(), f.getHeight(), f.getDeleteAfter(), created);
    }

    /** Lê todos os bytes de um upload multipart (uma foto por requisição — 12.3). */
    public static byte[] readAll(InputStream in) {
        try (in) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_PHOTO);
        }
    }
}
