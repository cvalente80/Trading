package dev.cvalente.trading.api;

import dev.cvalente.trading.paper.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/paper")
public class PaperController {
    private final PaperBroker broker;
    private final DecisionJournal journal;
    public PaperController(PaperBroker broker, DecisionJournal journal) { this.broker = broker; this.journal = journal; }
    public record OrderRequest(String symbol, OrderSide side, OrderType type, BigDecimal quantity, BigDecimal limitPrice) {}
    @GetMapping("/account") public PaperAccount account() { return broker.snapshot(); }
    @PostMapping("/orders") public PaperOrder order(@RequestBody OrderRequest request) {
        return broker.submit(request.symbol(), request.side(), request.type(), request.quantity(), request.limitPrice(), Instant.now());
    }
    @DeleteMapping("/orders/{id}") public void cancel(@PathVariable long id) { broker.cancel(id); }
    public record DecisionRequest(String symbol, String strategy, String action, int score, List<String> reasons) {}
    @PostMapping("/decisions") public DecisionJournal.Entry decision(@RequestBody DecisionRequest request) {
        return journal.append(Instant.now(), request.symbol(), request.strategy(), request.action(), request.score(), request.reasons());
    }
    @GetMapping("/decisions") public List<DecisionJournal.Entry> decisions() { return journal.recent(); }
}
