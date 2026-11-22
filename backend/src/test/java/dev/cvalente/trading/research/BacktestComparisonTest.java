package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BacktestComparisonTest {
    @Test void combinedReportUsesBothHalfCapitalSleeves() {
        var result = BacktestComparison.run(flatCandles(100), StrategyType.DAY, 10_000,
            BacktestConfig.defaults(.5, .001, .0005));
        assertEquals("DAY_LONG_SHORT", result.combined().strategy());
        assertEquals(10_000, result.combined().initialEquity(), 1e-9);
        assertEquals(10_000, result.combined().finalEquity(), 1e-9);
        assertEquals(result.longOnly().completedTrades() + result.shortOnly().completedTrades(),
            result.combined().completedTrades());
    }
    private static List<Candle> flatCandles(int count) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < count; i++) { BigDecimal p = BigDecimal.valueOf(100);
            result.add(new Candle(Instant.ofEpochSecond(i * 900L), p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p, BigDecimal.TEN, p.multiply(BigDecimal.TEN))); }
        return result;
    }
}
