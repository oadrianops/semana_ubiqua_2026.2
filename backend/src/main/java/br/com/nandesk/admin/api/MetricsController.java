package br.com.nandesk.admin.api;

import br.com.nandesk.admin.application.MetricsService;
import br.com.nandesk.admin.application.MetricsService.Overview;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores (RF-32) e métricas de campo web-vitals, anônimas e sem gravar no banco (9.14, RNF-02). */
@RestController
@Tag(name = "Indicadores")
public class MetricsController {

    private static final Set<String> NAMES = Set.of("LCP", "INP", "CLS", "TTFB", "FCP");
    private static final Set<String> CONNECTIONS = Set.of("slow-2g", "2g", "3g", "4g", "unknown");

    private final MetricsService metrics;
    private final MeterRegistry registry;

    public MetricsController(MetricsService metrics, MeterRegistry registry) {
        this.metrics = metrics;
        this.registry = registry;
    }

    public record Vital(
            @NotBlank String name,
            @NotNull @PositiveOrZero Double value,
            @Size(max = 60) String route,
            @Size(max = 20) String connection) {}

    public record VitalsBatch(@NotNull @Size(max = 10) List<@Valid Vital> metrics) {}

    @Operation(summary = "Painel de indicadores (RF-32)")
    @GetMapping("/api/v1/admin/metrics/overview")
    ResponseEntity<Overview> overview() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(metrics.overview());
    }

    @Operation(summary = "Métricas de campo (LCP, INP, CLS, TTFB) amostradas e anônimas (RNF-02)")
    @PostMapping("/api/v1/telemetry/web-vitals")
    ResponseEntity<Void> vitals(@Valid @RequestBody VitalsBatch body) {
        for (Vital v : body.metrics()) {
            if (!NAMES.contains(v.name())) {
                continue;
            }
            String connection =
                    v.connection() != null && CONNECTIONS.contains(v.connection()) ? v.connection() : "unknown";
            String route = v.route() == null ? "unknown" : v.route().replaceAll("[^a-z/:-]", "");
            DistributionSummary.builder("nandesk.web_vitals")
                    .tag("metric", v.name())
                    .tag("route", route)
                    .tag("connection", connection)
                    .publishPercentiles(0.75)
                    .register(registry)
                    .record(v.value());
        }
        return ResponseEntity.accepted().build();
    }
}
