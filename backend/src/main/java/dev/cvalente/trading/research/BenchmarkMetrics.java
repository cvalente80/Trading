package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import java.util.List;
import java.util.Random;

/** Baselines that use the same price interval as a strategy report. */
public final class BenchmarkMetrics {
    private BenchmarkMetrics() {}
    public static double buyAndHoldPercent(List<Candle> candles) {
        if (candles == null || candles.size() < 2) throw new IllegalArgumentException("At least two candles required");
        return 100 * (candles.get(candles.size() - 1).close().doubleValue() /
            candles.get(0).close().doubleValue() - 1);
    }

    /** Monte Carlo random long/flat baseline; seed makes comparisons reproducible. */
    public static double randomLongFlatPercent(List<Candle> candles, long seed, double participation) {
        if (candles == null || candles.size() < 2 || !Double.isFinite(participation) || participation < 0 || participation > 1)
            throw new IllegalArgumentException("Invalid random benchmark parameters");
        Random random = new Random(seed);
        double equity = 1;
        for (int i = 1; i < candles.size(); i++) {
            if (random.nextDouble() < participation)
                equity *= candles.get(i).close().doubleValue() / candles.get(i - 1).close().doubleValue();
        }
        return 100 * (equity - 1);
    }
}
