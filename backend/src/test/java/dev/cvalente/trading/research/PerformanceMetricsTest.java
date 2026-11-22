package dev.cvalente.trading.research;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMetricsTest {
    @Test void flatEquityHasNoDrawdown() {
        var tracker = new PerformanceMetrics.DrawdownTracker(10_000);
        tracker.observe(10_000);
        tracker.observe(10_000);
        assertEquals(0, tracker.maxDrawdownPercent(), 1e-12);
    }

    @Test void drawdownIsMeasuredFromPeakToTrough() {
        var tracker = new PerformanceMetrics.DrawdownTracker(10_000);
        tracker.observe(12_000);
        tracker.observe(9_000);
        tracker.observe(10_000);
        assertEquals(25, tracker.maxDrawdownPercent(), 1e-12);
    }

    @Test void drawdownDoesNotUseCurrentCloseAsPeakBeforeIntrabarLow() {
        var tracker = new PerformanceMetrics.DrawdownTracker(10_000);
        tracker.observe(9_000);   // adverse low of the candle
        tracker.observe(10_500);  // later close creates a new peak
        assertEquals(10, tracker.maxDrawdownPercent(), 1e-12);
    }

    @Test void winRateUsesOnlyCompletedTrades() {
        var metrics = PerformanceMetrics.trades(List.of(
            trade(10), trade(-5), trade(0)));
        assertEquals(100.0 / 3.0, metrics.winRatePercent(), 1e-12);
    }

    @Test void profitFactorIsGrossWinsDividedByGrossLosses() {
        var metrics = PerformanceMetrics.trades(List.of(
            trade(10), trade(20), trade(-5), trade(-5)));
        assertEquals(3, metrics.profitFactor(), 1e-12);
        assertEquals(5, metrics.expectancy(), 1e-12);
        assertEquals(15, metrics.averageGain(), 1e-12);
        assertEquals(5, metrics.averageLoss(), 1e-12);
    }

    @Test void profitFactorIsInfiniteWhenThereAreWinsAndNoLosses() {
        var metrics = PerformanceMetrics.trades(List.of(trade(10)));
        assertEquals(Double.POSITIVE_INFINITY, metrics.profitFactor());
    }

    @Test void emptyTradesHaveZeroWinRateAndProfitFactor() {
        var metrics = PerformanceMetrics.trades(List.of());
        assertEquals(0, metrics.winRatePercent(), 1e-12);
        assertEquals(0, metrics.profitFactor(), 1e-12);
        assertEquals(0, metrics.expectancy(), 1e-12);
    }

    @Test void tradeExposesOperationAnalysisFieldsAndDuration() {
        var trade = new ReplayBacktester.Trade(3, 7, Instant.EPOCH, Instant.ofEpochSecond(3600),
            100, 95, 110, 108, 2, 0.2, 0.21, 15.0, "TARGET");
        assertEquals(95, trade.stopLoss());
        assertEquals(110, trade.takeProfit());
        assertEquals(0.41, trade.entryFee() + trade.exitFee(), 1e-12);
        assertEquals(3600, trade.durationSeconds());
    }

    private static ReplayBacktester.Trade trade(double pnl) {
        return new ReplayBacktester.Trade(1, 2, Instant.EPOCH, Instant.ofEpochSecond(60),
            100, 98, 104, 101, 1, 1, 1, pnl, "TEST");
    }
}
