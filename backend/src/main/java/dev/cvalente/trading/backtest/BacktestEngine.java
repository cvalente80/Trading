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
        boolean pendingEnter = false, pendingExit = false;
        for (Bar bar : bars) {
            if (bar == null || bar.close() == null || bar.close().signum() <= 0)
                throw new IllegalArgumentException("Invalid bar");
            BigDecimal close = bar.close(); lastClose = close;
            // A signal observed on bar N is executed on bar N+1.
            if (pendingExit && qty.signum() > 0) {
                BigDecimal execution = close.multiply(BigDecimal.ONE.subtract(slippageRate), MC);
                BigDecimal gross = qty.multiply(execution, MC);
                BigDecimal fee = gross.multiply(feeRate, MC);
                fees = fees.add(fee); slippage = slippage.add(qty.multiply(close.subtract(execution), MC));
                cash = cash.add(gross.subtract(fee)); qty = BigDecimal.ZERO; trades++;
            } else if (pendingEnter && qty.signum() == 0) {
                BigDecimal execution = close.multiply(BigDecimal.ONE.add(slippageRate), MC);
                qty = cash.divide(execution.multiply(BigDecimal.ONE.add(feeRate), MC), MC);
                BigDecimal gross = qty.multiply(execution, MC);
                BigDecimal fee = gross.multiply(feeRate, MC);
                fees = fees.add(fee); slippage = slippage.add(qty.multiply(execution.subtract(close), MC));
                cash = cash.subtract(gross.add(fee));
            }
            pendingExit = bar.exit();
            pendingEnter = bar.enter();
        }
        BigDecimal mark = lastClose == null ? BigDecimal.ZERO : lastClose;
        BigDecimal liquidation = qty.multiply(mark.multiply(BigDecimal.ONE.subtract(slippageRate), MC), MC);
        BigDecimal liquidationFee = liquidation.multiply(feeRate, MC);
        BigDecimal equity = cash.add(liquidation.subtract(liquidationFee));
        return new Result(cash, equity, trades, fees, slippage);
    }
}
