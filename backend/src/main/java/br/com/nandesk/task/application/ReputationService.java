package br.com.nandesk.task.application;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.task.application.TaskEvents.AssignmentClosed;
import br.com.nandesk.task.domain.ReputationCalculator;
import br.com.nandesk.task.domain.TaskAssignment;
import br.com.nandesk.task.infra.TaskAssignmentRepository;
import br.com.nandesk.user.application.UserAccountService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Reputação do colaborador (RF-23, RN-17), recalculada depois do commit de cada tentativa encerrada. */
@Service
public class ReputationService {

    private final TaskAssignmentRepository assignments;
    private final UserAccountService users;
    private final ReputationCalculator calculator;
    private final int minimum;

    public ReputationService(TaskAssignmentRepository assignments, UserAccountService users, AppProperties properties) {
        this.assignments = assignments;
        this.users = users;
        AppProperties.Reputation r = properties.reputation();
        this.calculator = new ReputationCalculator(r.window(), r.initialScore(), r.minimumResults());
        this.minimum = r.minToAccept();
    }

    public record ReputationView(
            int score, int minimumToAccept, int window, long approved, long rejected, long expired) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssignmentClosed(AssignmentClosed event) {
        recompute(event.userId());
    }

    @Transactional
    public int recompute(Long userId) {
        int score = calculator.score(results(userId));
        users.setReputation(userId, score);
        return score;
    }

    @Transactional(readOnly = true)
    public ReputationView view(Long userId) {
        List<ReputationCalculator.Result> results = results(userId);
        int score = users.findProfile(userId).map(p -> p.getReputationScore()).orElse(calculator.initialScore());
        long approved = results.stream()
                .filter(r -> r.outcome().name().equals("APPROVED"))
                .count();
        long rejected = results.stream()
                .filter(r -> r.outcome().name().equals("REJECTED"))
                .count();
        long expired = results.stream()
                .filter(r -> r.outcome().name().equals("EXPIRED")
                        || (r.outcome().name().equals("RELEASED") && r.checkedIn()))
                .count();
        return new ReputationView(score, minimum, calculator.window(), approved, rejected, expired);
    }

    private List<ReputationCalculator.Result> results(Long userId) {
        List<TaskAssignment> closed = assignments.findClosedOf(userId, PageRequest.of(0, calculator.window() * 3));
        return closed.stream()
                .map(a -> new ReputationCalculator.Result(a.getOutcome(), a.isCheckedIn()))
                .toList();
    }
}
