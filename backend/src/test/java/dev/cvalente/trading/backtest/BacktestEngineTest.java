package dev.cvalente.trading.backtest;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {
    @Test void includesFeesAndSlippage() {
        var bars = List.of(
            new BacktestEngine.Bar(new BigDecimal("100"), true, false),
            new BacktestEngine.Bar(new BigDecimal("100"), false, true),
            new BacktestEngine.Bar(new BigDecimal("110"), false, false));
        var result = BacktestEngine.run(bars, new BigDecimal("10000"),
            new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertEquals(1, result.completedTrades());
        assertTrue(result.finalEquity().compareTo(new BigDecimal("10000")) > 0);
        assertTrue(result.totalFees().signum() > 0);
        assertTrue(result.totalSlippage().signum() > 0);
    }

    @Test void signalsExecuteOnFollowingBar() {
        var result = BacktestEngine.run(List.of(
            new BacktestEngine.Bar(new BigDecimal("100"), true, false),
            new BacktestEngine.Bar(new BigDecimal("100"), false, true),
            new BacktestEngine.Bar(new BigDecimal("110"), false, false)),
            new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(1, result.completedTrades());
        assertEquals(0, result.finalEquity().compareTo(new BigDecimal("11000")));
    }

    @Test void openPositionEquityIncludesLiquidationCosts() {
        var result = BacktestEngine.run(List.of(
            new BacktestEngine.Bar(new BigDecimal("100"), true, false),
            new BacktestEngine.Bar(new BigDecimal("110"), false, false)),
            new BigDecimal("10000"), new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertTrue(result.finalEquity().compareTo(new BigDecimal("11000")) < 0);
    }
}
