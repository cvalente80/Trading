package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CostSweepTest {
    @Test void returnsOneReportPerCost() {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 100; i++) { BigDecimal p = BigDecimal.valueOf(100);
            candles.add(new Candle(Instant.ofEpochSecond(i * 900L), p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p, BigDecimal.TEN, p.multiply(BigDecimal.TEN))); }
        var reports = CostSweep.run(candles, StrategyType.DAY, 10_000, .5, .0005, List.of(.0005, .001, .002));
        assertEquals(3, reports.size());
        assertEquals(BenchmarkMetrics.randomLongFlatPercent(candles, 42, .5), reports.get(0).randomBenchmarkPercent(), 1e-12);
    }
}
