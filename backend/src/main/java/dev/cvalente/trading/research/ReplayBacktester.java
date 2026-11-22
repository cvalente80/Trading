package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.market.BinancePublicMarketClient;
import dev.cvalente.trading.market.CandleSeriesValidator;
import dev.cvalente.trading.strategy.CrossoverSignalPolicy;
import dev.cvalente.trading.strategy.Indicators;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Educational, long-only, single-position replay. Decisions on closed bar, fills at next open. */
public final class ReplayBacktester {
    private ReplayBacktester() {}
    public record Trade(int entryIndex, int exitIndex, java.time.Instant entryTime,
                        java.time.Instant exitTime, double entry, double stopLoss,
                        double takeProfit, double exit, double quantity,
                        double entryFee, double exitFee, double netPnl, String reason) {
        public long durationSeconds() {
            return entryTime == null || exitTime == null ? 0 : java.time.Duration.between(entryTime, exitTime).getSeconds();
        }

        /** JSON bean accessor for clients such as the React operation analysis. */
        public long getDurationSeconds() {
            return durationSeconds();
        }
    }
    public record Report(String strategy, int candles, double initialEquity, double finalEquity,
                         double netReturnPercent, double buyHoldPercent, int completedTrades,
                         double randomBenchmarkPercent,
                         double winRatePercent, double profitFactor, double maxDrawdownPercent,
                         double totalFees, double expectancy, double averageGain, double averageLoss,
                         double averageDurationSeconds, int maxConsecutiveLosses,
                         List<Trade> trades, String note) {}

    public static Report run(List<Candle> candles, StrategyType strategy, double initial,
                             double riskPercent, double fee, double slippage) {
        return run(candles, strategy, initial, BacktestConfig.defaults(riskPercent, fee, slippage));
    }

    public static Report run(List<Candle> candles, StrategyType strategy, double initial, BacktestConfig config) {
        double riskPercent = config.riskPercent(), fee = config.fee(), slippage = config.slippage();
        if (candles == null || candles.size() < 70 || strategy == null || !Double.isFinite(initial) || initial <= 0 ||
            !Double.isFinite(riskPercent) || !Double.isFinite(fee) || !Double.isFinite(slippage) ||
            riskPercent <= 0 || riskPercent > 5 || fee < 0 || fee >= 0.1 ||
            slippage < 0 || slippage >= 0.1) throw new IllegalArgumentException("Invalid backtest parameters");
        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);
            if (c == null || c.openTime() == null || c.open() == null || c.high() == null ||
                c.low() == null || c.close() == null || c.volume() == null || c.quoteVolume() == null ||
                c.open().signum() <= 0 || c.high().compareTo(c.low()) < 0 ||
                c.low().signum() <= 0 || c.high().compareTo(c.open()) < 0 ||
                c.low().compareTo(c.open()) > 0 || c.high().compareTo(c.close()) < 0 ||
                c.low().compareTo(c.close()) > 0 || c.volume().signum() < 0 || c.quoteVolume().signum() < 0 || (i > 0 &&
                !c.openTime().isAfter(candles.get(i-1).openTime())))
                throw new IllegalArgumentException("Invalid or unordered candle at index " + i);
        }
        long expectedInterval = strategy == StrategyType.DAY
            ? BinancePublicMarketClient.intervalMillis("15m")
            : BinancePublicMarketClient.intervalMillis("4h");
        CandleSeriesValidator.validate(candles, expectedInterval);
        double cash = initial, qty = 0, entry = 0, stop = 0, target = 0, entryCost = 0, positionEntryFee = 0;
        int entryIndex = -1;
        double fees = 0;
        PerformanceMetrics.DrawdownTracker drawdown = new PerformanceMetrics.DrawdownTracker(initial);
        List<Trade> trades = new ArrayList<>();
        int fast = strategy == StrategyType.DAY ? 9 : 20;
        int slow = strategy == StrategyType.DAY ? 21 : 50;
        for (int i = 60; i < candles.size(); i++) {
            Candle current = candles.get(i);
            double open = current.open().doubleValue();
            // Stop/target of an existing position; adverse gap and same-bar stop priority.
            if (qty > 0) {
                Optional<ExecutionModel.Exit> decision = ExecutionModel.existingPosition(
                    open, current.low().doubleValue(), current.high().doubleValue(), stop, target, slippage);
                if (decision.isPresent()) {
                    String reason = decision.get().reason();
                    double exit = decision.get().price();
                    double gross = qty * exit, exitFee = gross * fee;
                    double pnl = gross - exitFee - entryCost;
                    cash += gross - exitFee; fees += exitFee;
                    trades.add(new Trade(entryIndex, i, candles.get(entryIndex).openTime(), current.openTime(),
                        entry, stop, target, exit, qty, positionEntryFee, exitFee, pnl, reason));
                    qty = 0;
                }
                if (qty > 0 && CrossoverSignalPolicy.decision(candles, i - 1, strategy) == CrossoverSignalPolicy.Decision.EXIT) {
                    double exit = open * (1 - slippage);
                    double gross = qty * exit, exitFee = gross * fee;
                    double pnl = gross - exitFee - entryCost;
                    cash += gross - exitFee; fees += exitFee;
                    trades.add(new Trade(entryIndex, i, candles.get(entryIndex).openTime(), current.openTime(),
                        entry, stop, target, exit, qty, positionEntryFee, exitFee, pnl, "BEARISH_CROSS"));
                    qty = 0;
                }
                if (qty > 0 && config.maxHoldingCandles() > 0 && i - entryIndex >= config.maxHoldingCandles()) {
                    double exit = open * (1 - slippage), gross = qty * exit, exitFee = gross * fee;
                    double pnl = gross - exitFee - entryCost;
                    cash += gross - exitFee; fees += exitFee;
                    trades.add(new Trade(entryIndex, i, candles.get(entryIndex).openTime(), current.openTime(), entry, stop,
                        target, exit, qty, positionEntryFee, exitFee, pnl, "TIME_EXIT"));
                    qty = 0;
                }
                if (qty > 0 && config.trailingStopAtr() > 0) {
                    double trail = current.high().doubleValue() - config.trailingStopAtr() * atr(candles, i, 14);
                    if (Double.isFinite(trail)) stop = Math.max(stop, trail);
                }
            }
            // Signals depend exclusively on the previous closed bar (i-1).
            if (qty == 0 && i > 61 && eligibleEntry(candles, i - 1, strategy, config)) {
                double atr = atr(candles, i-1, 14);
                if (atr > 0) {
                    double fill = open * (1 + slippage);
                    double distance = 2 * atr;
                    stop = fill - distance;
                    if (!Double.isFinite(stop) || stop <= 0) continue;
                    double stopExecution = stop * (1 - slippage);
                    double riskPerUnit = fill * (1 + fee) - stopExecution * (1 - fee);
                    if (!Double.isFinite(riskPerUnit) || riskPerUnit <= 0) continue;
                    double proposed = (cash * riskPercent / 100) / riskPerUnit;
                    double affordable = cash / (fill * (1 + fee));
                    double candidate = Math.min(proposed, affordable);
                    if (candidate > 0 && Double.isFinite(candidate)) {
                        qty = candidate; entry = fill; entryIndex = i;
                        target = fill + 2 * distance;
                        entryCost = qty * fill * (1 + fee);
                        positionEntryFee = qty * fill * fee;
                        fees += positionEntryFee; cash -= entryCost;
                        // Same bar may touch the stop. Conservative stop-first execution.
                        Optional<ExecutionModel.Exit> decision = ExecutionModel.entryBar(
                            open, current.low().doubleValue(), stop, slippage);
                        if (decision.isPresent()) {
                            double exit = decision.get().price();
                            double gross = qty * exit, exitFee = gross * fee;
                            double pnl = gross - exitFee - entryCost;
                            cash += gross - exitFee; fees += exitFee;
                            trades.add(new Trade(entryIndex, i, candles.get(entryIndex).openTime(), current.openTime(),
                                entry, stop, target, exit, qty, positionEntryFee, exitFee, pnl, decision.get().reason()));
                            qty = 0;
                        }
                    }
                }
            }
            if (qty > 0) {
                double intrabarEquity = cash + qty * current.low().doubleValue() * (1 - slippage) * (1 - fee);
                // The current close is not allowed to become a peak before the
                // adverse low of the same candle is observed.
                drawdown.observe(intrabarEquity);
            }
            double equity = cash + qty * current.close().doubleValue() * (1 - slippage) * (1 - fee);
            drawdown.observe(equity);
        }
        // Unrealized open position is marked to market, not counted as completed trade.
        double finalClose = candles.get(candles.size()-1).close().doubleValue();
        double finalEquity = cash + qty * finalClose * (1 - slippage) * (1 - fee);
        if (qty > 0) {
            double liquidation = qty * finalClose * (1 - slippage);
            fees += liquidation * fee;
        }
        double bh = 100 * (candles.get(candles.size()-1).close().doubleValue() /
            candles.get(0).close().doubleValue() - 1);
        PerformanceMetrics.TradeMetrics metrics = PerformanceMetrics.trades(trades);
        return new Report(strategy.name(), candles.size(), initial, finalEquity,
            100 * (finalEquity / initial - 1), bh, trades.size(),
            BenchmarkMetrics.randomLongFlatPercent(candles, 42L, .5),
            metrics.winRatePercent(), metrics.profitFactor(),
            drawdown.maxDrawdownPercent(), fees, metrics.expectancy(), metrics.averageGain(),
            metrics.averageLoss(), metrics.averageDurationSeconds(), metrics.maxConsecutiveLosses(),
            List.copyOf(trades),
            "Historical simulation; no guarantee. One position, next-open fills, stop-first ambiguity. " +
            "Profit factor is +Infinity when there are gains and no losing completed trades.");
    }

    private static boolean eligibleEntry(List<Candle> candles, int end, StrategyType strategy, BacktestConfig config) {
        if (CrossoverSignalPolicy.decision(candles, end, strategy) != CrossoverSignalPolicy.Decision.BUY) return false;
        int fast = CrossoverSignalPolicy.fastPeriod(strategy);
        double close = candles.get(end).close().doubleValue();
        double ema = Indicators.ema(candles.subList(0, end + 1), fast).doubleValue();
        if (config.maxEmaDistancePercent() < Double.POSITIVE_INFINITY &&
            100 * Math.abs(close - ema) / ema > config.maxEmaDistancePercent()) return false;
        if (config.rsiFilter()) {
            double rsi = Indicators.rsi(candles.subList(0, end + 1), 14).doubleValue();
            if (rsi < config.minRsi() || rsi > config.maxRsi()) return false;
        }
        if (config.volumeFilter()) {
            double avg = Indicators.averageQuoteVolume(candles.subList(0, end + 1), Math.min(20, end + 1)).doubleValue();
            if (candles.get(end).quoteVolume().doubleValue() <= avg) return false;
        }
        if (config.adxFilter() && adx(candles, end, 14) < config.minAdx()) return false;
        if (config.higherTimeframeTrend() && !higherTrend(candles, end, strategy)) return false;
        if (config.avoidBearishRegime() && !higherTrend(candles, end, strategy)) return false;
        return true;
    }

    private static boolean higherTrend(List<Candle> candles, int end, StrategyType strategy) {
        int step = strategy == StrategyType.DAY ? 4 : 6;
        List<Candle> sampled = new ArrayList<>();
        for (int i = step - 1; i <= end; i += step) sampled.add(candles.get(i));
        if (sampled.size() < 21) return false;
        return Indicators.ema(sampled, 9).compareTo(Indicators.ema(sampled, 21)) > 0;
    }

    private static double adx(List<Candle> candles, int end, int period) {
        if (end < period * 2) return 0;
        double tr = 0, movement = 0;
        for (int i = end - period + 1; i <= end; i++) {
            double high = candles.get(i).high().doubleValue(), low = candles.get(i).low().doubleValue();
            double prevClose = candles.get(i - 1).close().doubleValue();
            tr += Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            movement += Math.abs(candles.get(i).close().doubleValue() - prevClose);
        }
        return tr == 0 ? 0 : 100 * movement / tr;
    }

    static boolean bullish(List<Candle> c, int end, int fast, int slow) {
        return CrossoverSignalPolicy.decision(c, end, fast == 9 ? StrategyType.DAY : StrategyType.SWING)
            == CrossoverSignalPolicy.Decision.BUY;
    }
    private static double ema(List<Candle> c, int end, int period) {
        double alpha = 2.0 / (period + 1);
        double result = c.get(0).close().doubleValue();
        for (int i = 1; i <= end; i++) result = alpha * c.get(i).close().doubleValue() + (1-alpha)*result;
        return result;
    }
    private static double atr(List<Candle> c, int end, int period) {
        double total = 0;
        for (int i = end-period+1; i <= end; i++) {
            double high = c.get(i).high().doubleValue(), low = c.get(i).low().doubleValue();
            double prev = c.get(i-1).close().doubleValue();
            total += Math.max(high-low, Math.max(Math.abs(high-prev), Math.abs(low-prev)));
        }
        return total / period;
    }
}
