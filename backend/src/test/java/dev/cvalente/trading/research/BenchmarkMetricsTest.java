package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BenchmarkMetricsTest {
    @Test void buyAndHoldUsesTheSameInterval() {
        var candles = List.of(candle(100, 0), candle(110, 1));
        assertEquals(10, BenchmarkMetrics.buyAndHoldPercent(candles), 1e-12);
    }
    @Test void randomBenchmarkIsReproducible() {
        var candles = List.of(candle(100, 0), candle(110, 1), candle(100, 2));
        assertEquals(BenchmarkMetrics.randomLongFlatPercent(candles, 42, .5),
            BenchmarkMetrics.randomLongFlatPercent(candles, 42, .5), 1e-12);
    }
    private static Candle candle(int p, int i) { BigDecimal x = BigDecimal.valueOf(p);
        return new Candle(Instant.ofEpochSecond(i * 900L), x, x, x, x, BigDecimal.ONE, x); }
}
