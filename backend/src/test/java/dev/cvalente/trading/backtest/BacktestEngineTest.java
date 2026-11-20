package dev.cvalente.trading.backtest;

import dev.cvalente.trading.backtest.legacy.LegacyBacktestEngine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {
    @Test void includesFeesAndSlippage() {
        var bars = List.of(
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), true, false),
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), false, true),
            new LegacyBacktestEngine.Bar(new BigDecimal("110"), false, false));
        var result = LegacyBacktestEngine.run(bars, new BigDecimal("10000"),
            new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertEquals(1, result.completedTrades());
        assertTrue(result.finalEquity().compareTo(new BigDecimal("10000")) > 0);
        assertTrue(result.totalFees().signum() > 0);
        assertTrue(result.totalSlippage().signum() > 0);
    }

    @Test void signalsExecuteOnFollowingBar() {
        var result = LegacyBacktestEngine.run(List.of(
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), true, false),
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), false, true),
            new LegacyBacktestEngine.Bar(new BigDecimal("110"), false, false)),
            new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(1, result.completedTrades());
        assertEquals(0, result.finalEquity().compareTo(new BigDecimal("11000")));
    }

    @Test void openPositionEquityIncludesLiquidationCosts() {
        var result = LegacyBacktestEngine.run(List.of(
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), true, false),
            new LegacyBacktestEngine.Bar(new BigDecimal("110"), false, false)),
            new BigDecimal("10000"), new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertTrue(result.finalEquity().compareTo(new BigDecimal("11000")) < 0);
    }

    @Test void openPositionLiquidationFeeIsIncludedInTotalFees() {
        var result = LegacyBacktestEngine.run(List.of(
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), true, false),
            new LegacyBacktestEngine.Bar(new BigDecimal("110"), false, false)),
            new BigDecimal("10000"), new BigDecimal("0.001"), BigDecimal.ZERO);
        double expected = 2 * (10000.0 / (110.0 * 1.001)) * 110.0 * 0.001;
        assertEquals(expected, result.totalFees().doubleValue(), 1e-10);
    }

    @Test void increasingCostsCannotImproveFinalEquity() {
        var bars = List.of(
            new LegacyBacktestEngine.Bar(new BigDecimal("100"), true, false),
            new LegacyBacktestEngine.Bar(new BigDecimal("110"), false, false));
        var free = LegacyBacktestEngine.run(bars, new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO);
        var costly = LegacyBacktestEngine.run(bars, new BigDecimal("10000"),
            new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertTrue(costly.finalEquity().compareTo(free.finalEquity()) < 0);
    }
}
