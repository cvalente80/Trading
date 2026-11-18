package dev.cvalente.trading.backtest;

import dev.cvalente.trading.market.BinancePublicMarketClient;
import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.market.CandleSeriesValidator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CandleIntervalTest {
    @Test void validatesIntervalsAndLimits() {
        assertEquals(900000L, BinancePublicMarketClient.intervalMillis("15m"));
        assertEquals(86400000L, BinancePublicMarketClient.intervalMillis("1d"));
        assertDoesNotThrow(() -> BinancePublicMarketClient.validate("BTCUSDT", "4h", 10000));
        assertThrows(IllegalArgumentException.class, () -> BinancePublicMarketClient.validate("BTCUSDT", "3m", 500));
        assertThrows(IllegalArgumentException.class, () -> BinancePublicMarketClient.validate("BTCUSDT", "15m", 10001));
    }

    @Test void rejectsMissingCandleInterval() {
        var candles = List.of(candle(0), candle(900_000), candle(2_700_000));
        assertThrows(IllegalArgumentException.class, () -> CandleSeriesValidator.validate(candles, 900_000));
    }

    @Test void completeSeriesRequiresExpectedFirstOpenAndCount() {
        var candles = List.of(candle(1_000), candle(901_000));
        assertTrue(CandleSeriesValidator.isComplete(candles, Instant.ofEpochMilli(1_000), 900_000, 2));
        assertFalse(CandleSeriesValidator.isComplete(candles, Instant.ofEpochMilli(0), 900_000, 2));
        assertFalse(CandleSeriesValidator.isComplete(candles, Instant.ofEpochMilli(1_000), 900_000, 3));
    }

    private static Candle candle(long millis) {
        return new Candle(Instant.ofEpochMilli(millis), bd(100), bd(101), bd(99), bd(100), bd(1), bd(100));
    }

    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
}
