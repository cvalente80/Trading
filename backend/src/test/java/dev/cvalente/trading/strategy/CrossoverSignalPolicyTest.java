package dev.cvalente.trading.strategy;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrossoverSignalPolicyTest {
    @Test void reportsInsufficientDataBeforeTheSlowEmaExists() {
        assertEquals(CrossoverSignalPolicy.Decision.INSUFFICIENT_DATA,
            CrossoverSignalPolicy.latest(flat(10), StrategyType.DAY));
    }

    @Test void bearishCrossIsAnExitDecision() {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 45; i++) candles.add(candle(i, 100 + i));
        for (int i = 45; i < 100; i++) candles.add(candle(i, 145 - (i - 45) * 4));
        boolean foundExit = false;
        for (int i = 21; i < candles.size(); i++) {
            if (CrossoverSignalPolicy.decision(candles, i, StrategyType.DAY) == CrossoverSignalPolicy.Decision.EXIT) {
                foundExit = true;
                break;
            }
        }
        assertEquals(true, foundExit);
    }

    private static List<Candle> flat(int count) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) candles.add(candle(i, 100));
        return candles;
    }

    private static Candle candle(int index, double close) {
        BigDecimal c = BigDecimal.valueOf(close);
        return new Candle(Instant.ofEpochSecond(index * 900L), c, c.add(BigDecimal.ONE),
            c.subtract(BigDecimal.ONE), c, BigDecimal.TEN, c.multiply(BigDecimal.TEN));
    }
}
