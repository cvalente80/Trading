package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WalkForwardStudyTest {
    @Test void splitPreservesOrderAndAllCandles() {
        List<Candle> candles = candles(100);
        var window = WalkForwardStudy.split(candles, .6, .2);
        assertEquals(60, window.train().size());
        assertEquals(20, window.validation().size());
        assertEquals(20, window.outOfSample().size());
        assertEquals(candles.get(60), window.validation().get(0));
    }

    @Test void rejectsInvalidSplit() {
        assertThrows(IllegalArgumentException.class, () -> WalkForwardStudy.split(candles(100), .8, .3));
    }

    private static List<Candle> candles(int n) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < n; i++) { BigDecimal p = BigDecimal.valueOf(100 + i);
            result.add(new Candle(Instant.ofEpochSecond(i * 900L), p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p, BigDecimal.ONE, p)); }
        return result;
    }
}
