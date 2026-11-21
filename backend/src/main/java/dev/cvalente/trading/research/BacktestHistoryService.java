package dev.cvalente.trading.research;

import dev.cvalente.trading.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class BacktestHistoryService {
    private final BacktestRunRepository runs;
    private final BacktestTradeRepository trades;
    public BacktestHistoryService(BacktestRunRepository runs, BacktestTradeRepository trades) {
        this.runs = runs; this.trades = trades;
    }
    public record RunSummary(Long id, Instant createdAt, String symbol, String strategy,
        String interval, int candleCount, double feeRate, double slippageRate,
        double initialEquity, double finalEquity, double netReturnPercent,
        double buyHoldPercent, int completedTrades, double winRatePercent,
        double profitFactor, double maxDrawdownPercent, double totalFees,
        double expectancy, double averageGain, double averageLoss,
        double averageDurationSeconds, int maxConsecutiveLosses,
        double randomBenchmarkPercent) {}
    public record RunDetails(RunSummary summary, List<ReplayBacktester.Trade> trades, String note) {}
    private static RunSummary summary(BacktestRunEntity e) {
        return new RunSummary(e.id,e.createdAt,e.symbol,e.strategy,e.intervalCode,
            e.candleCount,e.feeRate,e.slippageRate,e.initialEquity,e.finalEquity,
            e.netReturnPercent,e.buyHoldPercent,e.completedTrades,e.winRatePercent,
            e.profitFactor,e.maxDrawdownPercent,e.totalFees,e.expectancy,e.averageGain,
            e.averageLoss,e.averageDurationSeconds,e.maxConsecutiveLosses,e.randomBenchmarkPercent);
    }
    @Transactional
    public Long save(String symbol, String interval, double fee, double slippage, ReplayBacktester.Report report) {
        return save(symbol, interval, fee, slippage, report,
            BacktestConfig.defaults(.5, fee, slippage));
    }
    @Transactional
    public Long save(String symbol, String interval, double fee, double slippage,
                     ReplayBacktester.Report report, BacktestConfig config) {
        BacktestRunEntity run = new BacktestRunEntity();
        run.createdAt = Instant.now(); run.symbol = symbol; run.strategy = report.strategy();
        run.intervalCode = interval; run.candleCount = report.candles();
        run.configuration = config.canonicalForm();
        run.strategyVersion = version(report.strategy(), interval, config);
        run.feeRate = fee; run.slippageRate = slippage;
        run.initialEquity = report.initialEquity(); run.finalEquity = report.finalEquity();
        run.netReturnPercent = report.netReturnPercent(); run.buyHoldPercent = report.buyHoldPercent();
        run.completedTrades = report.completedTrades(); run.winRatePercent = report.winRatePercent();
        run.profitFactor = report.profitFactor(); run.maxDrawdownPercent = report.maxDrawdownPercent();
        run.totalFees = report.totalFees(); run.expectancy = report.expectancy();
        run.averageGain = report.averageGain(); run.averageLoss = report.averageLoss();
        run.averageDurationSeconds = report.averageDurationSeconds();
        run.maxConsecutiveLosses = report.maxConsecutiveLosses();
        run.randomBenchmarkPercent = report.randomBenchmarkPercent(); run.note = report.note();
        run = runs.save(run);
        for (ReplayBacktester.Trade t : report.trades()) {
            BacktestTradeEntity trade = new BacktestTradeEntity();
            trade.run = run; trade.entryIndex = t.entryIndex(); trade.exitIndex = t.exitIndex();
            trade.entryTime = t.entryTime(); trade.exitTime = t.exitTime();
            trade.entryPrice = t.entry(); trade.stopLoss = t.stopLoss(); trade.takeProfit = t.takeProfit();
            trade.exitPrice = t.exit(); trade.quantity = t.quantity();
            trade.entryFee = t.entryFee(); trade.exitFee = t.exitFee();
            trade.netPnl = t.netPnl(); trade.reason = t.reason();
            trades.save(trade);
        }
        return run.id;
    }

    public static String version(String strategy, String interval, BacktestConfig config) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var input = (strategy + "|" + interval + "|" + config.canonicalForm())
                .getBytes(StandardCharsets.UTF_8);
            var hex = new StringBuilder();
            for (byte b : digest.digest(input)) hex.append(String.format("%02x", b));
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
    @Transactional(readOnly=true)
    public List<RunSummary> recent() {
        return runs.findTop50ByOrderByCreatedAtDescIdDesc().stream().map(BacktestHistoryService::summary).toList();
    }
    @Transactional(readOnly=true)
    public RunDetails detail(Long id) {
        BacktestRunEntity run = runs.findById(id).orElseThrow(() -> new IllegalArgumentException("Backtest not found: " + id));
        var results = trades.findByRunIdOrderByEntryIndexAsc(id).stream().map(t ->
            new ReplayBacktester.Trade(t.entryIndex,t.exitIndex,t.entryTime,t.exitTime,t.entryPrice,
                t.stopLoss == null ? 0 : t.stopLoss, t.takeProfit == null ? 0 : t.takeProfit,
                t.exitPrice,t.quantity,t.entryFee == null ? 0 : t.entryFee,
                t.exitFee == null ? 0 : t.exitFee,t.netPnl,t.reason)).toList();
        return new RunDetails(summary(run), results, run.note);
    }
}
