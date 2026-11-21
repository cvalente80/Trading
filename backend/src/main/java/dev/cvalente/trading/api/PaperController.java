package dev.cvalente.trading.api;

import dev.cvalente.trading.paper.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/paper")
public class PaperController {
    private final PaperBroker broker;
    public PaperController(PaperBroker broker) { this.broker = broker; }
    public record OrderRequest(String symbol, OrderSide side, OrderType type, BigDecimal quantity, BigDecimal limitPrice) {}
    @GetMapping("/account") public PaperAccount account() { return broker.snapshot(); }
    @PostMapping("/orders") public PaperOrder order(@RequestBody OrderRequest request) {
        return broker.submit(request.symbol(), request.side(), request.type(), request.quantity(), request.limitPrice(), Instant.now());
    }
    @DeleteMapping("/orders/{id}") public void cancel(@PathVariable long id) { broker.cancel(id); }
}
