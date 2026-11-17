package dev.cvalente.trading.backtest;

import dev.cvalente.trading.market.BinancePublicMarketClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CandleIntervalTest {
    @Test void validatesIntervalsAndLimits() {
        assertEquals(900000L, BinancePublicMarketClient.intervalMillis("15m"));
        assertEquals(86400000L, BinancePublicMarketClient.intervalMillis("1d"));
        assertDoesNotThrow(() -> BinancePublicMarketClient.validate("BTCUSDT", "4h", 10000));
        assertThrows(IllegalArgumentException.class, () -> BinancePublicMarketClient.validate("BTCUSDT", "3m", 500));
        assertThrows(IllegalArgumentException.class, () -> BinancePublicMarketClient.validate("BTCUSDT", "15m", 10001));
    }
}
