package dev.cvalente.trading.api;

import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.market.CandleHistoryService;
import dev.cvalente.trading.research.BacktestHistoryService;
import dev.cvalente.trading.research.ReplayBacktester;
import dev.cvalente.trading.research.BacktestConfig;
import dev.cvalente.trading.research.BacktestComparison;
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
        @RequestParam(defaultValue="0.0005") double slippage,
        @RequestParam(defaultValue="0.5") double riskPercent,
        @RequestParam(defaultValue="false") boolean higherTimeframeTrend,
        @RequestParam(defaultValue="false") boolean adxFilter,
        @RequestParam(defaultValue="false") boolean rsiFilter,
        @RequestParam(defaultValue="false") boolean volumeFilter,
        @RequestParam(defaultValue="true") boolean avoidBearishRegime,
        @RequestParam(defaultValue="false") boolean enableShort,
        @RequestParam(defaultValue="Infinity") double maxEmaDistancePercent,
        @RequestParam(defaultValue="20") double minAdx,
        @RequestParam(defaultValue="45") double minRsi,
        @RequestParam(defaultValue="68") double maxRsi) {
        String interval = strategy == StrategyType.DAY ? "15m" : "4h";
        var candleHistory = historyCandles.load(symbol, interval, candles);
        if (!candleHistory.complete()) throw new IllegalArgumentException("Cannot backtest incomplete candle history");
        var config = new BacktestConfig(riskPercent, fee, slippage, higherTimeframeTrend,
            adxFilter, rsiFilter, volumeFilter, avoidBearishRegime, enableShort, maxEmaDistancePercent, minAdx, minRsi, maxRsi);
        var report = enableShort ? dev.cvalente.trading.research.ShortReplayBacktester.run(candleHistory.candles(), strategy, 10000, config)
            : ReplayBacktester.run(candleHistory.candles(), strategy, 10000, config);
        Long id = history.save(symbol, interval, fee, slippage, report);
        return new SavedBacktest(id, report);
    }
    @GetMapping("/backtest/compare")
    public BacktestComparison.Result compare(@RequestParam(defaultValue="BTCUSDT") String symbol,
        @RequestParam(defaultValue="DAY") StrategyType strategy,
        @RequestParam(defaultValue="500") int candles,
        @RequestParam(defaultValue="0.001") double fee,
        @RequestParam(defaultValue="0.0005") double slippage,
        @RequestParam(defaultValue="0.5") double riskPercent) {
        String interval = strategy == StrategyType.DAY ? "15m" : "4h";
        var candleHistory = historyCandles.load(symbol, interval, candles);
        if (!candleHistory.complete()) throw new IllegalArgumentException("Cannot backtest incomplete candle history");
        return BacktestComparison.run(candleHistory.candles(), strategy, 10000,
            BacktestConfig.defaults(riskPercent, fee, slippage));
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
