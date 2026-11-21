package dev.cvalente.trading.research;

/** Explicit research controls; defaults preserve the original crossover replay. */
public record BacktestConfig(double riskPercent, double fee, double slippage,
                             boolean higherTimeframeTrend, boolean adxFilter,
                             boolean rsiFilter, boolean volumeFilter,
                             boolean avoidBearishRegime,
                             boolean enableShort,
                             double trailingStopAtr,
                             int maxHoldingCandles,
                             double maxEmaDistancePercent, double minAdx,
                             double minRsi, double maxRsi) {
    public BacktestConfig {
        if (!Double.isFinite(riskPercent) || riskPercent <= 0 || riskPercent > 5 ||
            !Double.isFinite(fee) || fee < 0 || fee >= .1 || !Double.isFinite(slippage) || slippage < 0 || slippage >= .1 ||
            Double.isNaN(trailingStopAtr) || trailingStopAtr < 0 || maxHoldingCandles < 0 ||
            Double.isNaN(maxEmaDistancePercent) || maxEmaDistancePercent < 0 ||
            !Double.isFinite(minAdx) || !Double.isFinite(minRsi) || !Double.isFinite(maxRsi) || minRsi < 0 || maxRsi > 100 || minRsi > maxRsi)
            throw new IllegalArgumentException("Invalid backtest configuration");
    }
    public static BacktestConfig defaults(double riskPercent, double fee, double slippage) {
        return new BacktestConfig(riskPercent, fee, slippage, false, false, false, false, false, false, 0, 0, Double.POSITIVE_INFINITY, 20, 45, 68);
    }
    public static BacktestConfig conservative(double fee, double slippage) {
        return defaults(.5, fee, slippage);
    }

    /** Stable, human-readable representation persisted with every research run. */
    public String canonicalForm() {
        return "riskPercent=" + riskPercent + ",fee=" + fee + ",slippage=" + slippage
            + ",higherTimeframeTrend=" + higherTimeframeTrend + ",adxFilter=" + adxFilter
            + ",rsiFilter=" + rsiFilter + ",volumeFilter=" + volumeFilter
            + ",avoidBearishRegime=" + avoidBearishRegime + ",enableShort=" + enableShort
            + ",trailingStopAtr=" + trailingStopAtr + ",maxHoldingCandles=" + maxHoldingCandles
            + ",maxEmaDistancePercent=" + maxEmaDistancePercent + ",minAdx=" + minAdx
            + ",minRsi=" + minRsi + ",maxRsi=" + maxRsi;
    }
}
