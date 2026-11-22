package dev.cvalente.trading.paper;

import dev.cvalente.trading.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class DecisionJournal {
    public record Entry(Long id, Instant decisionTime, String symbol, String strategy,
                        String action, int score, String reasons) {}
    private final PaperDecisionRepository repository;
    public DecisionJournal(PaperDecisionRepository repository) { this.repository = repository; }

    @Transactional
    public Entry append(Instant time, String symbol, String strategy, String action, int score, List<String> reasons) {
        if (time == null || symbol == null || symbol.isBlank() || strategy == null || strategy.isBlank() ||
            action == null || action.isBlank() || score < 0 || reasons == null)
            throw new IllegalArgumentException("Invalid decision journal entry");
        PaperDecisionEntity entity = new PaperDecisionEntity();
        entity.decisionTime = time; entity.symbol = symbol; entity.strategy = strategy;
        entity.action = action; entity.score = score; entity.reasons = String.join(" | ", reasons);
        entity = repository.save(entity);
        return toEntry(entity);
    }

    @Transactional(readOnly = true)
    public List<Entry> recent() { return repository.findTop100ByOrderByDecisionTimeDescIdDesc().stream().map(DecisionJournal::toEntry).toList(); }

    private static Entry toEntry(PaperDecisionEntity e) { return new Entry(e.id, e.decisionTime, e.symbol, e.strategy, e.action, e.score, e.reasons); }
}
