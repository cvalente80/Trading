package dev.cvalente.trading.backtest;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {
    @Test void includesFeesAndSlippage() {
        var bars = List.of(
            new BacktestEngine.Bar(new BigDecimal("100"), true, false),
            new BacktestEngine.Bar(new BigDecimal("110"), false, true));
        var result = BacktestEngine.run(bars, new BigDecimal("10000"),
            new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertEquals(1, result.completedTrades());
        assertTrue(result.finalEquity().compareTo(new BigDecimal("10000")) > 0);
        assertTrue(result.totalFees().signum() > 0);
        assertTrue(result.totalSlippage().signum() > 0);
    }
}
