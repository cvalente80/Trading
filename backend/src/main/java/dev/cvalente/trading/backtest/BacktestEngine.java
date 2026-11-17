package dev.cvalente.trading.backtest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/** Deterministic long-only close-to-close simulation. Educational baseline only. */
public final class BacktestEngine {
    private static final MathContext MC = MathContext.DECIMAL64;
    private BacktestEngine() {}

    public record Bar(BigDecimal close, boolean enter, boolean exit) {}
    public record Result(BigDecimal finalCash, BigDecimal finalEquity, int completedTrades,
                         BigDecimal totalFees, BigDecimal totalSlippage) {}

    public static Result run(List<Bar> bars, BigDecimal initialCash,
                             BigDecimal feeRate, BigDecimal slippageRate) {
        if (bars == null || initialCash == null || initialCash.signum() <= 0 ||
            feeRate == null || slippageRate == null || feeRate.signum() < 0 ||
            slippageRate.signum() < 0 || feeRate.compareTo(BigDecimal.ONE) >= 0 ||
            slippageRate.compareTo(BigDecimal.ONE) >= 0)
            throw new IllegalArgumentException("Invalid backtest inputs");
        BigDecimal cash = initialCash, qty = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO, slippage = BigDecimal.ZERO;
        int trades = 0;
        BigDecimal lastClose = null;
        for (Bar bar : bars) {
            if (bar == null || bar.close() == null || bar.close().signum() <= 0)
                throw new IllegalArgumentException("Invalid bar");
            BigDecimal close = bar.close(); lastClose = close;
            // Signals are assumed to have been computed before the next bar.
            // The caller must enforce that to prevent look-ahead bias.
            if (qty.signum() > 0 && bar.exit()) {
                BigDecimal execution = close.multiply(BigDecimal.ONE.subtract(slippageRate), MC);
                BigDecimal gross = qty.multiply(execution, MC);
                BigDecimal fee = gross.multiply(feeRate, MC);
                fees = fees.add(fee); slippage = slippage.add(qty.multiply(close.subtract(execution), MC));
                cash = cash.add(gross.subtract(fee)); qty = BigDecimal.ZERO; trades++;
            } else if (qty.signum() == 0 && bar.enter()) {
                BigDecimal execution = close.multiply(BigDecimal.ONE.add(slippageRate), MC);
                qty = cash.divide(execution.multiply(BigDecimal.ONE.add(feeRate), MC), MC);
                BigDecimal gross = qty.multiply(execution, MC);
                BigDecimal fee = gross.multiply(feeRate, MC);
                fees = fees.add(fee); slippage = slippage.add(qty.multiply(execution.subtract(close), MC));
                cash = cash.subtract(gross.add(fee));
            }
        }
        BigDecimal equity = cash.add(qty.multiply(lastClose == null ? BigDecimal.ZERO : lastClose, MC));
        return new Result(cash, equity, trades, fees, slippage);
    }
}
