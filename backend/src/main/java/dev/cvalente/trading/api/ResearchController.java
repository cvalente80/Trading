package dev.cvalente.trading.api;

import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.market.CandleHistoryService;
import dev.cvalente.trading.research.BacktestHistoryService;
import dev.cvalente.trading.research.ReplayBacktester;
import dev.cvalente.trading.research.BacktestConfig;
import dev.cvalente.trading.research.BacktestComparison;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/research")
@CrossOrigin(origins = "http://localhost:5174")
public class ResearchController {
    private final CandleHistoryService historyCandles;
    private final BacktestHistoryService history;
    public ResearchController(CandleHistoryService historyCandles, BacktestHistoryService history) {
        this.historyCandles = historyCandles; this.history = history;
    }
    public record SavedBacktest(Long id, ReplayBacktester.Report report,
                                String strategyVersion, String configuration) {}
    public record StudyCell(String symbol, String interval, String period, String configuration,
                            int candles, double netReturnPercent, double buyHoldPercent,
                            double maxDrawdownPercent, double profitFactor, double expectancy,
                            int completedTrades, double winRatePercent) {}
    public record WalkForwardResult(String strategy, int requestedCandles, double trainFraction,
                                    double validationFraction, List<StudyCell> cells) {}
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
        @RequestParam(defaultValue="0") double trailingStopAtr,
        @RequestParam(defaultValue="0") int maxHoldingCandles,
        @RequestParam(defaultValue="Infinity") double maxEmaDistancePercent,
        @RequestParam(defaultValue="20") double minAdx,
        @RequestParam(defaultValue="45") double minRsi,
        @RequestParam(defaultValue="68") double maxRsi) {
        String interval = strategy == StrategyType.DAY ? "15m" : "4h";
        var candleHistory = historyCandles.load(symbol, interval, candles);
        if (!candleHistory.complete()) throw new IllegalArgumentException("Cannot backtest incomplete candle history");
        var config = new BacktestConfig(riskPercent, fee, slippage, higherTimeframeTrend,
            adxFilter, rsiFilter, volumeFilter, avoidBearishRegime, enableShort, trailingStopAtr, maxHoldingCandles,
            maxEmaDistancePercent, minAdx, minRsi, maxRsi);
        var report = enableShort ? dev.cvalente.trading.research.ShortReplayBacktester.run(candleHistory.candles(), strategy, 10000, config)
            : ReplayBacktester.run(candleHistory.candles(), strategy, 10000, config);
        Long id = history.save(symbol, interval, fee, slippage, report, config);
        return new SavedBacktest(id, report, BacktestHistoryService.version(report.strategy(), interval, config), config.canonicalForm());
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

    @GetMapping("/walk-forward")
    public WalkForwardResult walkForward(
        @RequestParam(defaultValue="BTCUSDT,ETHUSDT,SOLUSDT") String symbols,
        @RequestParam(defaultValue="DAY") StrategyType strategy,
        @RequestParam(defaultValue="1000") int candles,
        @RequestParam(defaultValue="0.6") double trainFraction,
        @RequestParam(defaultValue="0.2") double validationFraction,
        @RequestParam(defaultValue="0.001") double fee,
        @RequestParam(defaultValue="0.0005") double slippage,
        @RequestParam(defaultValue="0.5") double riskPercent,
        @RequestParam(defaultValue="false") boolean conservative) {
        if (symbols == null || symbols.isBlank() || symbols.split(",").length > 8)
            throw new IllegalArgumentException("Use between 1 and 8 symbols");
        String interval = strategy == StrategyType.DAY ? "15m" : "4h";
        BacktestConfig config = new BacktestConfig(riskPercent, fee, slippage,
            conservative, conservative, conservative, conservative, true, false,
            conservative ? 2 : 0, conservative ? 96 : 0,
            conservative ? 3 : Double.POSITIVE_INFINITY, 20, 45, 68);
        List<StudyCell> cells = new ArrayList<>();
        for (String raw : symbols.split(",")) {
            String symbol = raw.trim().toUpperCase();
            if (symbol.isBlank()) continue;
            var history = historyCandles.load(symbol, interval, candles);
            if (!history.complete()) throw new IllegalArgumentException("Incomplete candle history for " + symbol);
            var window = dev.cvalente.trading.research.WalkForwardStudy.split(history.candles(), trainFraction, validationFraction);
            var reports = dev.cvalente.trading.research.WalkForwardStudy.evaluate(window, strategy, 10000, config);
            String[] periods = {"TRAIN", "VALIDATION", "OUT_OF_SAMPLE"};
            for (int i = 0; i < reports.size(); i++) {
                var r = reports.get(i);
                cells.add(new StudyCell(symbol, interval, periods[i], conservative ? "CONSERVATIVE" : "BASELINE",
                    r.candles(), r.netReturnPercent(), r.buyHoldPercent(), r.maxDrawdownPercent(),
                    r.profitFactor(), r.expectancy(), r.completedTrades(), r.winRatePercent()));
            }
        }
        return new WalkForwardResult(strategy.name(), candles, trainFraction, validationFraction, List.copyOf(cells));
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

    @GetMapping(value = "/backtests/{id}/trades.csv", produces = "text/csv")
    public String tradesCsv(@PathVariable Long id) {
        var details = history.detail(id);
        StringBuilder csv = new StringBuilder("entryIndex,exitIndex,entryTime,exitTime,entry,stopLoss,takeProfit,exit,quantity,entryFee,exitFee,netPnl,reason,durationSeconds\n");
        for (var trade : details.trades()) {
            csv.append(trade.entryIndex()).append(',').append(trade.exitIndex()).append(',')
                .append(value(trade.entryTime())).append(',').append(value(trade.exitTime())).append(',')
                .append(trade.entry()).append(',').append(trade.stopLoss()).append(',').append(trade.takeProfit()).append(',')
                .append(trade.exit()).append(',').append(trade.quantity()).append(',').append(trade.entryFee()).append(',')
                .append(trade.exitFee()).append(',').append(trade.netPnl()).append(',')
                .append(csvValue(trade.reason())).append(',').append(trade.durationSeconds()).append('\n');
        }
        return csv.toString();
    }

    private static String value(Object value) { return value == null ? "" : value.toString(); }
    private static String csvValue(String value) {
        if (value == null) return "";
        return value.contains(",") || value.contains("\"") || value.contains("\n")
            ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
