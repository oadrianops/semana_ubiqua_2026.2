package br.com.nandesk.task.api;

import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.task.application.TaskAdminQueries;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.task.application.TaskViews.TaskDetail;
import br.com.nandesk.task.application.TaskViews.TaskSummary;
import br.com.nandesk.task.domain.TaskOrigin;
import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.task.domain.UpdateTask;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Criação, publicação e cancelamento manual de tarefas pela moderação (RF-13). */
@RestController
@Tag(name = "Moderação")
public class TaskModerationController {

    private final TaskService tasks;
    private final TaskAdminQueries queries;
    private final MarketQueryService markets;
    private final StallAdminService stalls;
    private final UserAccountService users;

    public TaskModerationController(
            TaskService tasks,
            TaskAdminQueries queries,
            MarketQueryService markets,
            StallAdminService stalls,
            UserAccountService users) {
        this.tasks = tasks;
        this.queries = queries;
        this.markets = markets;
        this.stalls = stalls;
        this.users = users;
    }

    public record CreateTaskRequest(
            @NotNull TaskType type,
            @NotBlank String marketSlug,
            UUID stallPublicId,
            String sectorSlug,
            @Size(max = 500) String instructions,
            @Min(1) @Max(3) Integer priority) {}

    public record CancelRequest(@Size(max = 255) String reason) {}

    public record PublishRequest(boolean volunteer) {}

    @Operation(summary = "Criar tarefa manual (RF-13)")
    @PostMapping("/api/v1/moderation/tasks")
    ResponseEntity<TaskDetail> create(@Valid @RequestBody CreateTaskRequest body) {
        Market market = markets.findActive(body.marketSlug()).orElseThrow(BusinessException::notFound);
        Stall stall = body.stallPublicId() == null ? null : stalls.require(body.stallPublicId());
        if (stall != null && !stall.getMarket().getId().equals(market.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Sector sector = body.sectorSlug() == null
                ? null
                : markets.sector(market, body.sectorSlug()).orElse(null);
        Long actor = users.require(CurrentUser.require().publicId()).getId();
        UpdateTask task = tasks.create(
                body.type(),
                TaskOrigin.MANUAL,
                market,
                stall,
                sector,
                body.instructions(),
                body.priority() == null ? 2 : body.priority(),
                actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(tasks.detail(task, null, AppLocale.current()));
    }

    @Operation(summary = "Publicar tarefa em espera, com orçamento ou como voluntária (RN-10)")
    @PostMapping("/api/v1/moderation/tasks/{publicId}/publish")
    ResponseEntity<Void> publish(@PathVariable UUID publicId, @RequestBody(required = false) PublishRequest body) {
        Long actor = users.require(CurrentUser.require().publicId()).getId();
        tasks.publish(publicId, body != null && body.volunteer(), actor);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancelar tarefa (RF-13)")
    @PostMapping("/api/v1/moderation/tasks/{publicId}/cancel")
    ResponseEntity<Void> cancel(@PathVariable UUID publicId, @RequestBody(required = false) CancelRequest body) {
        Long actor = users.require(CurrentUser.require().publicId()).getId();
        tasks.cancel(publicId, body == null ? null : body.reason(), actor);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Tarefas para o painel (RF-13)")
    @GetMapping("/api/v1/admin/tasks")
    ResponseEntity<PageResponse<TaskSummary>> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long marketId = market == null
                ? null
                : markets.findActive(market).map(Market::getId).orElse(null);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(queries.search(status, marketId, page, size, AppLocale.current()));
    }
}
