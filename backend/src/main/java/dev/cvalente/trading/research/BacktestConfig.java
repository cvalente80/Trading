package dev.cvalente.trading.research;

/** Explicit research controls; defaults preserve the original crossover replay. */
public record BacktestConfig(double riskPercent, double fee, double slippage,
                             boolean higherTimeframeTrend, boolean adxFilter,
                             boolean rsiFilter, boolean volumeFilter,
                             boolean avoidBearishRegime,
                             double maxEmaDistancePercent, double minAdx,
                             double minRsi, double maxRsi) {
    public BacktestConfig {
        if (!Double.isFinite(riskPercent) || riskPercent <= 0 || riskPercent > 5 ||
            !Double.isFinite(fee) || fee < 0 || fee >= .1 || !Double.isFinite(slippage) || slippage < 0 || slippage >= .1 ||
            Double.isNaN(maxEmaDistancePercent) || maxEmaDistancePercent < 0 ||
            !Double.isFinite(minAdx) || !Double.isFinite(minRsi) || !Double.isFinite(maxRsi) || minRsi < 0 || maxRsi > 100 || minRsi > maxRsi)
            throw new IllegalArgumentException("Invalid backtest configuration");
    }
    public static BacktestConfig defaults(double riskPercent, double fee, double slippage) {
        return new BacktestConfig(riskPercent, fee, slippage, false, false, false, false, false, Double.POSITIVE_INFINITY, 20, 45, 68);
    }
    public static BacktestConfig conservative(double fee, double slippage) {
        return defaults(.5, fee, slippage);
    }
}
