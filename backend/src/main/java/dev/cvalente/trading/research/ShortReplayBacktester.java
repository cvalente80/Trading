package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.strategy.CrossoverSignalPolicy;

import java.util.ArrayList;
import java.util.List;

/** Conservative short-only replay with next-open fills and the same report contract as long replay. */
public final class ShortReplayBacktester {
    private ShortReplayBacktester() {}

    public static ReplayBacktester.Report run(List<Candle> candles, StrategyType strategy, double initial,
                                              BacktestConfig config) {
        if (candles == null || candles.size() < 70 || strategy == null || !Double.isFinite(initial) || initial <= 0)
            throw new IllegalArgumentException("Invalid short backtest inputs");
        double cash = initial, qty = 0, entry = 0, stop = 0, target = 0, entryCost = 0, entryFee = 0, fees = 0;
        int entryIndex = -1;
        List<ReplayBacktester.Trade> trades = new ArrayList<>();
        PerformanceMetrics.DrawdownTracker drawdown = new PerformanceMetrics.DrawdownTracker(initial);
        for (int i = 62; i < candles.size(); i++) {
            Candle current = candles.get(i);
            double open = current.open().doubleValue(), high = current.high().doubleValue(), low = current.low().doubleValue();
            if (qty > 0) {
                double exit = Double.NaN; String reason = null;
                if (high >= stop) { exit = stop * (1 + config.slippage()); reason = "STOP"; }
                else if (low <= target) { exit = target * (1 - config.slippage()); reason = "TARGET"; }
                else if (CrossoverSignalPolicy.decision(candles, i - 1, strategy) == CrossoverSignalPolicy.Decision.BUY) {
                    exit = open * (1 + config.slippage()); reason = "BULLISH_CROSS";
                }
                if (reason != null) {
                    double gross = qty * (entry - exit), exitFee = qty * exit * config.fee();
                    double pnl = gross - exitFee - entryCost;
                    cash += qty * entry + pnl; fees += exitFee;
                    trades.add(new ReplayBacktester.Trade(entryIndex, i, candles.get(entryIndex).openTime(), current.openTime(),
                        entry, stop, target, exit, -qty, entryFee, exitFee, pnl, reason));
                    qty = 0;
                }
            }
            if (qty == 0 && CrossoverSignalPolicy.decision(candles, i - 1, strategy) == CrossoverSignalPolicy.Decision.EXIT) {
                double fill = open * (1 - config.slippage());
                double atr = atr(candles, i - 1, 14), distance = 2 * atr;
                stop = fill + distance; target = fill - 2 * distance;
                double riskPerUnit = stop * (1 + config.fee()) - fill * (1 - config.fee());
                if (atr > 0 && riskPerUnit > 0) {
                    qty = Math.min((cash * config.riskPercent() / 100) / riskPerUnit,
                        cash / (fill * (1 + config.fee())));
                    if (qty > 0 && Double.isFinite(qty)) {
                        entry = fill; entryIndex = i; entryCost = qty * fill * config.fee();
                        entryFee = entryCost; fees += entryFee; cash -= entryCost;
                    }
                }
            }
            double equity = cash + (qty > 0 ? qty * (entry - current.close().doubleValue()) : 0);
            drawdown.observe(Math.max(0, equity));
        }
        double finalClose = candles.get(candles.size() - 1).close().doubleValue();
        double finalEquity = cash + (qty > 0 ? qty * (entry - finalClose) : 0);
        PerformanceMetrics.TradeMetrics metrics = PerformanceMetrics.trades(trades);
        double buyHold = BenchmarkMetrics.buyAndHoldPercent(candles);
        return new ReplayBacktester.Report(strategy.name() + "_SHORT", candles.size(), initial, finalEquity,
            100 * (finalEquity / initial - 1), buyHold, trades.size(),
            BenchmarkMetrics.randomLongFlatPercent(candles, 42, .5), metrics.winRatePercent(), metrics.profitFactor(),
            drawdown.maxDrawdownPercent(), fees, metrics.expectancy(), metrics.averageGain(), metrics.averageLoss(),
            metrics.averageDurationSeconds(), metrics.maxConsecutiveLosses(), List.copyOf(trades),
            "Short-only historical simulation; next-open fills, conservative stop-first policy.");
    }

    private static double atr(List<Candle> candles, int end, int period) {
        double total = 0;
        for (int i = end - period + 1; i <= end; i++) {
            double high = candles.get(i).high().doubleValue(), low = candles.get(i).low().doubleValue();
            double previous = candles.get(i - 1).close().doubleValue();
            total += Math.max(high - low, Math.max(Math.abs(high - previous), Math.abs(low - previous)));
        }
        return total / period;
    }
}
