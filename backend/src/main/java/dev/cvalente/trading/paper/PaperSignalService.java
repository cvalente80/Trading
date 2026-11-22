package dev.cvalente.trading.paper;

import dev.cvalente.trading.domain.*;
import dev.cvalente.trading.strategy.SignalEngine;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PaperSignalService {
    public record Evaluation(Signal signal, DecisionJournal.Entry journalEntry, PaperOrder order) {}
    private final SignalEngine signalEngine;
    private final PaperBroker broker;
    private final DecisionJournal journal;

    public PaperSignalService(SignalEngine signalEngine, PaperBroker broker, DecisionJournal journal) {
        this.signalEngine = signalEngine; this.broker = broker; this.journal = journal;
    }

    public Evaluation evaluate(String symbol, StrategyType strategy, List<Candle> candles) {
        Signal signal = signalEngine.evaluate(symbol, strategy, candles);
        var entry = journal.append(signal.generatedAt(), symbol, strategy.name(), signal.action(), signal.score(), signal.reasons());
        PaperOrder order = null;
        if ("BUY_CANDIDATE".equals(signal.action()) && signal.referencePrice() != null && signal.suggestedStop() != null) {
            BigDecimal quantity = PositionSizer.quantity(broker.snapshot().cash(), BigDecimal.valueOf(.5),
                signal.referencePrice(), signal.suggestedStop());
            if (quantity.signum() > 0) order = broker.submit(symbol, OrderSide.BUY, OrderType.MARKET, quantity, null, signal.generatedAt());
        } else if ("BEARISH_EXIT".equals(signal.action())) {
            quantityFor(broker, symbol).ifPresent(quantity -> {
                if (quantity.signum() > 0) broker.submit(symbol, OrderSide.SELL, OrderType.MARKET, quantity, null, signal.generatedAt());
            });
        }
        return new Evaluation(signal, entry, order);
    }

    private static java.util.Optional<BigDecimal> quantityFor(PaperBroker broker, String symbol) {
        return broker.snapshot().positions().stream().filter(position -> position.symbol().equals(symbol))
            .map(PaperPosition::quantity).findFirst();
    }
}
