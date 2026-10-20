package br.com.nandesk.stall.application;

import br.com.nandesk.stall.infra.StallRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contagens de boxes para os indicadores (OBJ-1, OBJ-3 — RF-32). */
@Service
@Transactional(readOnly = true)
public class StallMetrics {

    private final StallRepository stalls;

    public StallMetrics(StallRepository stalls) {
        this.stalls = stalls;
    }

    public long publicStalls() {
        return stalls.countPublic();
    }

    public long publicVerifiedSince(Instant since) {
        return stalls.countPublicVerifiedSince(since);
    }

    public long publicWithPhoto() {
        return stalls.countPublicWithPhoto();
    }
}
