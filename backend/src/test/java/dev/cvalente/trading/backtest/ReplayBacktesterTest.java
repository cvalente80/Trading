package dev.cvalente.trading.backtest;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.research.ReplayBacktester;
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
    private static BigDecimal bd(int n) { return BigDecimal.valueOf(n); }
}
