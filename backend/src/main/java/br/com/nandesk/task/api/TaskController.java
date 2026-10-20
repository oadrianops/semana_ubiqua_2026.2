package br.com.nandesk.task.api;

import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.task.application.TaskViews.AcceptResult;
import br.com.nandesk.task.application.TaskViews.AvailableTasks;
import br.com.nandesk.task.application.TaskViews.CheckInResult;
import br.com.nandesk.task.application.TaskViews.TaskDetail;
import br.com.nandesk.task.domain.TaskType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tarefas do colaborador: lista, aceite, check-in e desistência (RF-15 a RF-17, RF-19). */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Colaboração")
public class TaskController {

    private final TaskService tasks;
    private final MarketQueryService markets;

    public TaskController(TaskService tasks, MarketQueryService markets) {
        this.tasks = tasks;
        this.markets = markets;
    }

    public record CheckInRequest(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotNull @PositiveOrZero Double accuracyMeters) {}

    @Operation(summary = "Tarefas abertas, por distância quando a posição é informada (não é gravada — RN-21)")
    @GetMapping("/available")
    ResponseEntity<AvailableTasks> available(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        Long marketId = market == null
                ? null
                : markets.findActive(market)
                        .orElseThrow(BusinessException::notFound)
                        .getId();
        return noStore(
                tasks.available(CurrentUser.require().publicId(), marketId, type, lat, lng, AppLocale.current()));
    }

    @Operation(summary = "Detalhe da tarefa (RF-15)")
    @GetMapping("/{publicId}")
    ResponseEntity<TaskDetail> detail(@PathVariable UUID publicId) {
        return noStore(tasks.detail(publicId, CurrentUser.require().publicId(), AppLocale.current()));
    }

    @Operation(summary = "Aceitar — passo 1 de 3 (RF-16, RN-01)")
    @PostMapping("/{publicId}/accept")
    ResponseEntity<AcceptResult> accept(@PathVariable UUID publicId) {
        return noStore(tasks.accept(publicId, CurrentUser.require().publicId()));
    }

    @Operation(summary = "Fazer check-in — passo 2 de 3 (RF-17, RN-05)")
    @PostMapping("/{publicId}/check-in")
    ResponseEntity<CheckInResult> checkIn(@PathVariable UUID publicId, @Valid @RequestBody CheckInRequest body) {
        return noStore(tasks.checkIn(
                publicId, CurrentUser.require().publicId(), body.latitude(), body.longitude(), body.accuracyMeters()));
    }

    @Operation(summary = "Desistir da tarefa (RF-19)")
    @PostMapping("/{publicId}/release")
    ResponseEntity<Void> release(@PathVariable UUID publicId) {
        tasks.release(publicId, CurrentUser.require().publicId());
        return ResponseEntity.noContent().build();
    }

    static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
