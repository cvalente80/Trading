package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReplayBacktesterTest {
    @Test void flatMarketHasNoTradesAndNoDrawdown() {
        List<Candle> candles = new ArrayList<>();
        for (int i=0;i<100;i++) candles.add(new Candle(Instant.ofEpochSecond(i*900L),
            bd(100), bd(101), bd(99), bd(100), bd(10), bd(1000)));
        var report = ReplayBacktester.run(candles, StrategyType.DAY,10000,1,0.001,0.0005);
        assertEquals(0, report.completedTrades());
        assertEquals(10000, report.finalEquity(), 0.00001);
        assertEquals(0, report.maxDrawdownPercent(), 0.00001);
    }
    @Test void invalidRiskRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            ReplayBacktester.run(List.of(),StrategyType.DAY,10000,10,0.001,0.0005));
    }

    @Test void nonFiniteParametersAreRejected() {
        var candles = flatCandles(100);
        assertThrows(IllegalArgumentException.class, () ->
            ReplayBacktester.run(candles, StrategyType.DAY, Double.NaN, 1, 0.001, 0.0005));
        assertThrows(IllegalArgumentException.class, () ->
            ReplayBacktester.run(candles, StrategyType.DAY, 10000, Double.POSITIVE_INFINITY, 0.001, 0.0005));
        assertThrows(IllegalArgumentException.class, () ->
            ReplayBacktester.run(candles, StrategyType.DAY, 10000, 1, Double.NaN, 0.0005));
    }

    @Test void gapsInExpectedDayCandlesAreRejected() {
        var candles = flatCandles(100);
        candles.remove(50);
        assertThrows(IllegalArgumentException.class, () ->
            ReplayBacktester.run(candles, StrategyType.DAY, 10000, 1, 0.001, 0.0005));
    }

    @Test void futureCandlesCannotChangeEarlierSignalDecision() {
        var original = flatCandles(100);
        var changedFuture = new ArrayList<>(original);
        changedFuture.set(99, new Candle(Instant.ofEpochSecond(99 * 900L),
            bd(10_000), bd(10_001), bd(9_999), bd(10_000), bd(10), bd(100_000)));
        assertEquals(ReplayBacktester.bullish(original, 80, 9, 21),
            ReplayBacktester.bullish(changedFuture, 80, 9, 21));
    }

    private static List<Candle> flatCandles(int count) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) candles.add(new Candle(Instant.ofEpochSecond(i * 900L),
            bd(100), bd(101), bd(99), bd(100), bd(10), bd(1000)));
        return candles;
    }
    private static BigDecimal bd(int n) { return BigDecimal.valueOf(n); }
}
