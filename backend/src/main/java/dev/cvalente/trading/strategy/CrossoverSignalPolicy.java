package dev.cvalente.trading.strategy;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;

import java.util.List;

/** Single source of truth for crossover decisions used by live signals and replay. */
public final class CrossoverSignalPolicy {
    public enum Decision { BUY, EXIT, NONE, INSUFFICIENT_DATA }

    private CrossoverSignalPolicy() {}

    public static Decision decision(List<Candle> candles, int end, StrategyType strategy) {
        int fast = fastPeriod(strategy), slow = slowPeriod(strategy);
        if (candles == null || strategy == null || end < slow || end >= (candles == null ? 0 : candles.size()))
            return Decision.INSUFFICIENT_DATA;
        double f = Indicators.ema(candles.subList(0, end + 1), fast).doubleValue();
        double s = Indicators.ema(candles.subList(0, end + 1), slow).doubleValue();
        double pf = Indicators.ema(candles.subList(0, end), fast).doubleValue();
        double ps = Indicators.ema(candles.subList(0, end), slow).doubleValue();
        boolean bullish = f > s && pf <= ps && candles.get(end).close().doubleValue() > f;
        boolean bearish = f < s && pf >= ps;
        if (bullish) return Decision.BUY;
        if (bearish) return Decision.EXIT;
        return Decision.NONE;
    }

    public static Decision latest(List<Candle> candles, StrategyType strategy) {
        if (candles == null || candles.isEmpty()) return Decision.INSUFFICIENT_DATA;
        return decision(candles, candles.size() - 1, strategy);
    }

    public static int fastPeriod(StrategyType strategy) { return strategy == StrategyType.DAY ? 9 : 20; }
    public static int slowPeriod(StrategyType strategy) { return strategy == StrategyType.DAY ? 21 : 50; }
}
