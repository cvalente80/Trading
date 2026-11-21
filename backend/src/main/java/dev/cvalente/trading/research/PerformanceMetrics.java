package dev.cvalente.trading.research;

import java.util.List;

/** Pure performance calculations shared by the replay report and unit tests. */
public final class PerformanceMetrics {
    private PerformanceMetrics() {}

    public record TradeMetrics(double winRatePercent, double profitFactor, double expectancy,
                               double averageGain, double averageLoss, double averageDurationSeconds,
                               int maxConsecutiveLosses) {}

    public static TradeMetrics trades(List<ReplayBacktester.Trade> trades) {
        double wins = 0, lossesCount = 0, gains = 0, losses = 0, duration = 0;
        int consecutiveLosses = 0, maxConsecutiveLosses = 0;
        for (ReplayBacktester.Trade trade : trades) {
            duration += trade.durationSeconds();
            if (trade.netPnl() > 0) {
                wins++;
                gains += trade.netPnl();
                consecutiveLosses = 0;
            } else if (trade.netPnl() < 0) {
                lossesCount++;
                losses += -trade.netPnl();
                maxConsecutiveLosses = Math.max(maxConsecutiveLosses, ++consecutiveLosses);
            }
        }
        double winRate = trades.isEmpty() ? 0 : 100 * wins / trades.size();
        double profitFactor = losses == 0 ? (gains > 0 ? Double.POSITIVE_INFINITY : 0) : gains / losses;
        double expectancy = trades.isEmpty() ? 0 : (gains - losses) / trades.size();
        return new TradeMetrics(winRate, profitFactor,
            expectancy, wins == 0 ? 0 : gains / wins, lossesCount == 0 ? 0 : losses / lossesCount,
            trades.isEmpty() ? 0 : duration / trades.size(), maxConsecutiveLosses);
    }

    public static final class DrawdownTracker {
        private double peak;
        private double maxDrawdownPercent;

        public DrawdownTracker(double initialEquity) {
            if (!Double.isFinite(initialEquity) || initialEquity <= 0) {
                throw new IllegalArgumentException("Invalid initial equity");
            }
            peak = initialEquity;
        }

        public void observe(double equity) {
            if (!Double.isFinite(equity) || equity < 0) {
                throw new IllegalArgumentException("Invalid equity");
            }
            if (equity > peak) peak = equity;
            maxDrawdownPercent = Math.max(maxDrawdownPercent, 100 * (peak - equity) / peak);
        }

        public double maxDrawdownPercent() {
            return maxDrawdownPercent;
        }
    }
}
