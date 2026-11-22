package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compares independent long-only, short-only and 50/50 combined sleeves. */
public final class BacktestComparison {
    private BacktestComparison() {}
    public record Result(ReplayBacktester.Report longOnly, ReplayBacktester.Report shortOnly,
                         ReplayBacktester.Report combined) {}

    public static Result run(List<Candle> candles, StrategyType strategy, double initial, BacktestConfig config) {
        double sleeveInitial = initial / 2;
        var longOnly = ReplayBacktester.run(candles, strategy, sleeveInitial, config);
        var shortOnly = ShortReplayBacktester.run(candles, strategy, sleeveInitial, config);
        var trades = new ArrayList<ReplayBacktester.Trade>();
        trades.addAll(longOnly.trades()); trades.addAll(shortOnly.trades());
        trades.sort(Comparator.comparingInt(ReplayBacktester.Trade::entryIndex));
        var metrics = PerformanceMetrics.trades(trades);
        double finalEquity = longOnly.finalEquity() + shortOnly.finalEquity();
        return new Result(longOnly, shortOnly, new ReplayBacktester.Report(
            strategy.name() + "_LONG_SHORT", candles.size(), initial, finalEquity,
            100 * (finalEquity / initial - 1), BenchmarkMetrics.buyAndHoldPercent(candles),
            trades.size(), BenchmarkMetrics.randomLongFlatPercent(candles, 42, .5), metrics.winRatePercent(),
            metrics.profitFactor(), Math.max(longOnly.maxDrawdownPercent(), shortOnly.maxDrawdownPercent()),
            longOnly.totalFees() + shortOnly.totalFees(), metrics.expectancy(), metrics.averageGain(),
            metrics.averageLoss(), metrics.averageDurationSeconds(), metrics.maxConsecutiveLosses(),
            List.copyOf(trades), "Combined 50/50 long-only and short-only sleeves."));
    }
}
