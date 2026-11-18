package dev.cvalente.trading.api;

import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.market.CandleHistoryService;
import dev.cvalente.trading.research.BacktestHistoryService;
import dev.cvalente.trading.research.ReplayBacktester;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/research")
@CrossOrigin(origins = "http://localhost:5174")
public class ResearchController {
    private final CandleHistoryService historyCandles;
    private final BacktestHistoryService history;
    public ResearchController(CandleHistoryService historyCandles, BacktestHistoryService history) {
        this.historyCandles = historyCandles; this.history = history;
    }
    public record SavedBacktest(Long id, ReplayBacktester.Report report) {}
    @GetMapping("/backtest")
    public SavedBacktest backtest(@RequestParam(defaultValue="BTCUSDT") String symbol,
        @RequestParam(defaultValue="DAY") StrategyType strategy,
        @RequestParam(defaultValue="500") int candles,
        @RequestParam(defaultValue="0.001") double fee,
        @RequestParam(defaultValue="0.0005") double slippage) {
        String interval = strategy == StrategyType.DAY ? "15m" : "4h";
        var candleHistory = historyCandles.load(symbol, interval, candles);
        if (!candleHistory.complete()) throw new IllegalArgumentException("Cannot backtest incomplete candle history");
        var report = ReplayBacktester.run(candleHistory.candles(),
            strategy, 10000, 1, fee, slippage);
        Long id = history.save(symbol, interval, fee, slippage, report);
        return new SavedBacktest(id, report);
    }
    @GetMapping("/candles")
    public CandleHistoryService.History candles(@RequestParam(defaultValue="BTCUSDT") String symbol,
        @RequestParam(defaultValue="15m") String interval,
        @RequestParam(defaultValue="500") int limit) {
        return historyCandles.load(symbol,interval,limit);
    }
    @GetMapping("/backtests")
    public List<BacktestHistoryService.RunSummary> recent() { return history.recent(); }
    @GetMapping("/backtests/{id}")
    public BacktestHistoryService.RunDetails detail(@PathVariable Long id) { return history.detail(id); }
}
