package br.com.nandesk.stall.application;

import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.config.CacheConfig;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.domain.Availability;
import br.com.nandesk.stall.domain.ConsentMethod;
import br.com.nandesk.stall.domain.ConsentStatus;
import br.com.nandesk.stall.domain.InstagramHandle;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.stall.domain.StallOffering;
import br.com.nandesk.stall.domain.StallPhoto;
import br.com.nandesk.stall.domain.StallStatus;
import br.com.nandesk.stall.infra.StallOfferingRepository;
import br.com.nandesk.stall.infra.StallPhotoRepository;
import br.com.nandesk.stall.infra.StallRepository;
import br.com.nandesk.user.domain.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escrita de boxes e ofertas (RF-29) e aplicação das mudanças aprovadas. Ocultar um box (inativação ou revogação
 * do consentimento) publica {@link StallHidden}, que cancela as tarefas ativas no módulo de tarefas (RN-13).
 */
@Service
public class StallAdminService {

    private final StallRepository stalls;
    private final StallOfferingRepository offerings;
    private final StallPhotoRepository photos;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public StallAdminService(
            StallRepository stalls,
            StallOfferingRepository offerings,
            StallPhotoRepository photos,
            ApplicationEventPublisher events,
            Clock clock) {
        this.stalls = stalls;
        this.offerings = offerings;
        this.photos = photos;
        this.events = events;
        this.clock = clock;
    }

    /** Evento: box saiu da vitrine (inativado ou consentimento revogado). */
    public record StallHidden(Long stallId, Long marketId, String reason) {}

    /** Evento: dados públicos de um box mudaram (para invalidar caches e incrementar {@code dataVersion}). */
    public record StallChanged(Long stallId, Long marketId) {}

    public record OfferingCommand(
            Product product, BigDecimal referencePrice, PriceUnit priceUnit, Availability availability) {}

    public record AdminStallView(
            UUID publicId,
            String marketSlug,
            String code,
            String displayName,
            String vendorName,
            boolean showVendorName,
            String sectorSlug,
            String sectorName,
            StallStatus status,
            ConsentStatus consentStatus,
            ConsentMethod consentMethod,
            Instant consentRecordedAt,
            Instant lastVerifiedAt,
            int version,
            boolean demo,
            String instagram,
            String photoThumbUrl) {}

    /** Foto no painel: a principal (capa do box) ou da galeria, com o id para remover. */
    public record AdminPhoto(UUID fileId, String url, String thumbUrl, boolean main) {}

    /** Box completo para edição no painel: dados, ofertas e fotos (RF-29). */
    public record AdminStallDetail(
            AdminStallView stall, List<StallViews.OfferingView> offerings, List<AdminPhoto> photos) {}

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public Stall createStall(
            Market market,
            Sector sector,
            String code,
            String displayName,
            String vendorName,
            boolean showVendorName,
            ConsentMethod method,
            User recordedBy) {
        return createStall(
                market, sector, code, displayName, vendorName, showVendorName, method, recordedBy, null, false);
    }

    /** Box cadastrado no painel a partir do termo assinado na feira, ou pelo seed (RF-29, RN-13). */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public Stall createStall(
            Market market,
            Sector sector,
            String code,
            String displayName,
            String vendorName,
            boolean showVendorName,
            ConsentMethod method,
            User recordedBy,
            String instagram,
            boolean demo) {
        if (stalls.findByMarketIdAndCode(market.getId(), code).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Stall stall = new Stall(market, sector, code, displayName, method, recordedBy, clock.instant());
        stall.setVendorName(vendorName);
        stall.setShowVendorName(showVendorName && vendorName != null);
        stall.setInstagramHandle(instagram(instagram));
        stall.setDemo(demo);
        Stall saved = stalls.save(stall);
        events.publishEvent(new StallChanged(saved.getId(), market.getId()));
        return saved;
    }

    public Optional<Stall> findByMarketAndCode(Market market, String code) {
        return stalls.findByMarketIdAndCode(market.getId(), code);
    }

    /** Cria ou atualiza a oferta de um produto no box (seed, painel e aplicação do changeset). */
    @Transactional
    public StallOffering putOffering(Stall stall, OfferingCommand cmd, Instant verifiedAt) {
        StallOffering offering = stall.getOfferings().stream()
                .filter(o -> o.getProduct().getId().equals(cmd.product().getId()))
                .findFirst()
                .orElse(null);
        if (offering == null) {
            offering = new StallOffering(
                    stall, cmd.product(), cmd.referencePrice(), cmd.priceUnit(), cmd.availability(), verifiedAt);
            stall.getOfferings().add(offering);
        } else {
            offering.setReferencePrice(cmd.referencePrice());
            offering.setPriceUnit(cmd.priceUnit());
            offering.setAvailability(cmd.availability());
            offering.setLastVerifiedAt(verifiedAt);
        }
        return offering;
    }

    @Transactional
    public void removeOffering(Stall stall, Product product) {
        stall.getOfferings().removeIf(o -> o.getProduct().getId().equals(product.getId()));
    }

    /** Confirma uma oferta sem mudança: só renova a data de verificação. */
    @Transactional
    public void confirmOffering(Stall stall, Product product, Instant verifiedAt) {
        stall.getOfferings().stream()
                .filter(o -> o.getProduct().getId().equals(product.getId()))
                .forEach(o -> o.setLastVerifiedAt(verifiedAt));
    }

    @Transactional
    public void markVerified(Stall stall, Instant verifiedAt) {
        stall.setLastVerifiedAt(verifiedAt);
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
    }

    @Transactional
    public void setPhoto(Stall stall, StoredFile photo) {
        stall.setPhoto(photo);
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public AdminStallView update(
            UUID publicId,
            String displayName,
            String vendorName,
            boolean showVendorName,
            Sector sector,
            StallStatus status,
            String instagram,
            Boolean demo,
            int expectedVersion) {
        Stall stall = stalls.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
        if (stall.getVersion() != expectedVersion) {
            throw new BusinessException(ErrorCode.STALE_VERSION);
        }
        stall.setDisplayName(displayName);
        stall.setVendorName(vendorName);
        stall.setShowVendorName(showVendorName && vendorName != null);
        stall.setSector(sector);
        if (instagram != null) {
            // Ausente mantém o perfil; texto vazio apaga.
            stall.setInstagramHandle(instagram(instagram));
        }
        if (demo != null) {
            stall.setDemo(demo);
        }
        boolean wasPublic = stall.isPublic();
        stall.setStatus(status);
        if (wasPublic && !stall.isPublic()) {
            events.publishEvent(new StallHidden(stall.getId(), stall.getMarket().getId(), "STALL_INACTIVE"));
        }
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
        return view(stall, AppLocale.PT_BR);
    }

    /** Revogação do consentimento: oculta na hora, cancela tarefas e agenda a remoção das fotos (RN-13). */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public AdminStallView revokeConsent(UUID publicId, java.time.Duration photoRetention) {
        Stall stall = stalls.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
        if (stall.getConsentStatus() == ConsentStatus.REVOKED) {
            return view(stall, AppLocale.PT_BR);
        }
        Instant now = clock.instant();
        stall.revokeConsent(now);
        if (stall.getPhoto() != null) {
            stall.getPhoto().setDeleteAfter(now.plus(photoRetention));
        }
        photos.findByStallIdOrderBySortOrderAscIdAsc(stall.getId())
                .forEach(p -> p.getFile().setDeleteAfter(now.plus(photoRetention)));
        events.publishEvent(new StallHidden(stall.getId(), stall.getMarket().getId(), "CONSENT_REVOKED"));
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
        return view(stall, AppLocale.PT_BR);
    }

    /** Retenção vencida da foto de um box revogado (RN-13): a foto sai do box antes de ser apagada. */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public int clearPhotos(java.util.Collection<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return 0;
        }
        return stalls.clearPhotos(fileIds) + photos.deleteByFileIds(fileIds);
    }

    /** Novo consentimento registrado presencialmente (RN-13). */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public AdminStallView grantConsent(UUID publicId, ConsentMethod method, User recordedBy) {
        Stall stall = stalls.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
        stall.setConsentStatus(ConsentStatus.GRANTED);
        stall.setConsentMethod(method);
        stall.setConsentRecordedAt(clock.instant());
        stall.setConsentRecordedBy(recordedBy);
        stall.setConsentRevokedAt(null);
        if (stall.getPhoto() != null) {
            // Consentimento de volta: a foto deixa de estar agendada para remoção (RN-13).
            stall.getPhoto().setDeleteAfter(null);
        }
        photos.findByStallIdOrderBySortOrderAscIdAsc(stall.getId())
                .forEach(p -> p.getFile().setDeleteAfter(null));
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
        return view(stall, AppLocale.PT_BR);
    }

    /** Seed: garante a marca de exemplo dos boxes fictícios também em bancos já carregados (ADR-0018). */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public void markDemo(Stall stall, boolean demo) {
        if (stall.isDemo() != demo) {
            stall.setDemo(demo);
            events.publishEvent(
                    new StallChanged(stall.getId(), stall.getMarket().getId()));
        }
    }

    /** Oferta conferida em campo e lançada no painel: conta como verificação do box (RF-29, RN-12). */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public void saveOffering(UUID publicId, OfferingCommand cmd) {
        Stall stall = require(publicId);
        Instant now = clock.instant();
        putOffering(stall, cmd, now);
        markVerified(stall, now);
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public void deleteOffering(UUID publicId, Product product) {
        Stall stall = require(publicId);
        removeOffering(stall, product);
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
    }

    /**
     * Foto enviada pelo painel (já pública): a principal vira a foto do box e as demais entram no fim da galeria.
     * A principal anterior vai para a galeria, para nada se perder sem querer.
     */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public void addPhoto(UUID publicId, StoredFile file, boolean main) {
        Stall stall = require(publicId);
        if (main) {
            StoredFile previous = stall.getPhoto();
            stall.setPhoto(file);
            if (previous != null) {
                appendToGallery(stall, previous);
            }
        } else {
            appendToGallery(stall, file);
        }
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
    }

    /** Põe a foto no fim da galeria pública do box (painel e envios aprovados — RF-22). */
    @Transactional
    public void appendToGallery(Stall stall, StoredFile file) {
        photos.save(new StallPhoto(stall, file, photos.maxSortOrder(stall.getId()) + 1, clock.instant()));
    }

    /** Tira a foto do box e deixa o arquivo para a retenção apagar (RN-13). */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public void removePhoto(UUID publicId, UUID fileId) {
        Stall stall = require(publicId);
        Instant now = clock.instant();
        if (stall.getPhoto() != null && stall.getPhoto().getPublicId().equals(fileId)) {
            stall.getPhoto().setDeleteAfter(now);
            stall.setPhoto(null);
        } else {
            StallPhoto photo =
                    photos.findByStallIdAndFilePublicId(stall.getId(), fileId).orElseThrow(BusinessException::notFound);
            photo.getFile().setDeleteAfter(now);
            photos.delete(photo);
        }
        events.publishEvent(new StallChanged(stall.getId(), stall.getMarket().getId()));
    }

    @Transactional(readOnly = true)
    public AdminStallDetail detail(UUID publicId, AppLocale locale) {
        Stall stall = require(publicId);
        List<StallViews.OfferingView> offeringViews = offerings.findByStallWithProduct(stall.getId()).stream()
                .sorted(java.util.Comparator.comparing(o -> o.getProduct().name(locale)))
                .map(o -> StallQueryService.offering(o, locale))
                .toList();
        List<AdminPhoto> all = new java.util.ArrayList<>();
        if (stall.getPhoto() != null) {
            all.add(adminPhoto(stall.getPhoto(), true));
        }
        photos.findByStallIdOrderBySortOrderAscIdAsc(stall.getId())
                .forEach(p -> all.add(adminPhoto(p.getFile(), false)));
        return new AdminStallDetail(view(stall, locale), offeringViews, all);
    }

    private static AdminPhoto adminPhoto(StoredFile file, boolean main) {
        return new AdminPhoto(file.getPublicId(), file.url(false), file.url(true), main);
    }

    private static String instagram(String raw) {
        try {
            return InstagramHandle.normalize(raw).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminStallView> search(Market market, String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                stalls.searchAdmin(
                        market == null ? null : market.getId(),
                        like,
                        PageRequest.of(Math.max(0, page), PageResponse.clampSize(size), Sort.by("code"))),
                s -> view(s, AppLocale.PT_BR));
    }

    @Transactional(readOnly = true)
    public Stall require(UUID publicId) {
        return stalls.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
    }

    @Transactional(readOnly = true)
    public List<StallOffering> offeringsOf(Stall stall) {
        return offerings.findByStallWithProduct(stall.getId());
    }

    @Transactional(readOnly = true)
    public List<Stall> staleStalls(Instant before) {
        return stalls.findPublicStale(before);
    }

    /** Ofertas do produto de origem migram para o destino numa mescla de catálogo (RF-30). */
    @Transactional
    public int migrateOfferings(Product source, Product target) {
        List<StallOffering> all = offerings.findAll().stream()
                .filter(o -> o.getProduct().getId().equals(source.getId()))
                .toList();
        int moved = 0;
        for (StallOffering o : all) {
            Stall stall = o.getStall();
            boolean targetExists = stall.getOfferings().stream()
                    .anyMatch(x -> x.getProduct().getId().equals(target.getId()));
            if (targetExists) {
                stall.getOfferings().remove(o);
            } else {
                o.setProduct(target);
            }
            moved++;
        }
        return moved;
    }

    public AdminStallView view(Stall s, AppLocale locale) {
        return new AdminStallView(
                s.getPublicId(),
                s.getMarket().getSlug(),
                s.getCode(),
                s.getDisplayName(),
                s.getVendorName(),
                s.isShowVendorName(),
                s.getSector() == null ? null : s.getSector().getSlug(),
                s.getSector() == null ? null : s.getSector().name(locale),
                s.getStatus(),
                s.getConsentStatus(),
                s.getConsentMethod(),
                s.getConsentRecordedAt(),
                s.getLastVerifiedAt(),
                s.getVersion(),
                s.isDemo(),
                s.getInstagramHandle(),
                s.getPhoto() == null ? null : s.getPhoto().url(true));
    }
}
