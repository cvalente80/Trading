package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ShortReplayBacktesterTest {
    @Test void shortReportUsesShortStrategyAndSharedBenchmarks() {
        var report = ShortReplayBacktester.run(flatCandles(100), StrategyType.DAY, 10_000,
            new BacktestConfig(.5, .001, .0005, false, false, false, false, false, true, 0, 0, Double.POSITIVE_INFINITY, 20, 45, 68));
        assertEquals("DAY_SHORT", report.strategy());
        assertEquals(100, report.candles());
        assertTrue(Double.isFinite(report.randomBenchmarkPercent()));
        assertEquals(0, report.completedTrades());
    }
    private static List<Candle> flatCandles(int count) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < count; i++) { BigDecimal p = BigDecimal.valueOf(100);
            result.add(new Candle(Instant.ofEpochSecond(i * 900L), p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p, BigDecimal.TEN, p.multiply(BigDecimal.TEN))); }
        return result;
    }
}
