package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Educational, long-only, single-position replay. Decisions on closed bar, fills at next open. */
public final class ReplayBacktester {
    private ReplayBacktester() {}
    public record Trade(int entryIndex, int exitIndex, double entry, double exit,
                        double quantity, double netPnl, String reason) {}
    public record Report(String strategy, int candles, double initialEquity, double finalEquity,
                         double netReturnPercent, double buyHoldPercent, int completedTrades,
                         double winRatePercent, double profitFactor, double maxDrawdownPercent,
                         double totalFees, List<Trade> trades, String note) {}

    public static Report run(List<Candle> candles, StrategyType strategy, double initial,
                             double riskPercent, double fee, double slippage) {
        if (candles == null || candles.size() < 70 || strategy == null || initial <= 0 ||
            riskPercent <= 0 || riskPercent > 5 || fee < 0 || fee >= 0.1 ||
            slippage < 0 || slippage >= 0.1) throw new IllegalArgumentException("Invalid backtest parameters");
        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);
            if (c == null || c.openTime() == null || c.open() == null || c.high() == null ||
                c.low() == null || c.close() == null || c.volume() == null || c.quoteVolume() == null ||
                c.open().signum() <= 0 || c.high().compareTo(c.low()) < 0 ||
                c.low().signum() <= 0 || c.high().compareTo(c.open()) < 0 ||
                c.low().compareTo(c.open()) > 0 || c.high().compareTo(c.close()) < 0 ||
                c.low().compareTo(c.close()) > 0 || c.volume().signum() < 0 || c.quoteVolume().signum() < 0 || (i > 0 &&
                !c.openTime().isAfter(candles.get(i-1).openTime())))
                throw new IllegalArgumentException("Invalid or unordered candle at index " + i);
        }
        double cash = initial, qty = 0, entry = 0, stop = 0, target = 0, entryCost = 0;
        int entryIndex = -1;
        double fees = 0, peak = initial, drawdown = 0, wins = 0, gains = 0, losses = 0;
        List<Trade> trades = new ArrayList<>();
        int fast = strategy == StrategyType.DAY ? 9 : 20;
        int slow = strategy == StrategyType.DAY ? 21 : 50;
        for (int i = 60; i < candles.size(); i++) {
            Candle current = candles.get(i);
            double open = current.open().doubleValue();
            // Stop/target of an existing position; adverse gap and same-bar stop priority.
            if (qty > 0) {
                String reason = null;
                double exit = 0;
                if (open <= stop) { exit = open * (1 - slippage); reason = "STOP_GAP"; }
                else if (current.low().doubleValue() <= stop) { exit = stop * (1 - slippage); reason = "STOP"; }
                else if (current.high().doubleValue() >= target) { exit = target * (1 - slippage); reason = "TARGET"; }
                if (reason != null) {
                    double gross = qty * exit, exitFee = gross * fee;
                    double pnl = gross - exitFee - entryCost;
                    cash += gross - exitFee; fees += exitFee;
                    trades.add(new Trade(entryIndex, i, entry, exit, qty, pnl, reason));
                    if (pnl > 0) { wins++; gains += pnl; } else losses -= pnl;
                    qty = 0;
                }
            }
            // Signals depend exclusively on the previous closed bar (i-1).
            if (qty == 0 && i > 61 && bullish(candles, i-1, fast, slow)) {
                double atr = atr(candles, i-1, 14);
                if (atr > 0) {
                    double fill = open * (1 + slippage);
                    double distance = 2 * atr;
                    double stopExecution = stop * (1 - slippage);
                    double riskPerUnit = fill * (1 + fee) - stopExecution * (1 - fee);
                    double proposed = (cash * riskPercent / 100) / riskPerUnit;
                    double affordable = cash / (fill * (1 + fee));
                    double candidate = Math.min(proposed, affordable);
                    if (candidate > 0 && Double.isFinite(candidate)) {
                        qty = candidate; entry = fill; entryIndex = i;
                        stop = fill - distance; target = fill + 2 * distance;
                        if (stop <= 0) { qty = 0; continue; }
                        entryCost = qty * fill * (1 + fee);
                        double entryFee = qty * fill * fee;
                        fees += entryFee; cash -= entryCost;
                        // Same bar may touch the stop. Conservative stop-first execution.
                        if (current.low().doubleValue() <= stop) {
                            double exit = (open <= stop ? open : stop) * (1 - slippage);
                            double gross = qty * exit, exitFee = gross * fee;
                            double pnl = gross - exitFee - entryCost;
                            cash += gross - exitFee; fees += exitFee;
                            trades.add(new Trade(entryIndex, i, entry, exit, qty, pnl, "ENTRY_BAR_STOP"));
                            if (pnl > 0) { wins++; gains += pnl; } else losses -= pnl;
                            qty = 0;
                        }
                    }
                }
            }
            double equity = cash + qty * current.close().doubleValue() * (1 - slippage) * (1 - fee);
            peak = Math.max(peak, equity);
            drawdown = Math.max(drawdown, 100 * (peak - equity) / peak);
            if (qty > 0) {
                double intrabarEquity = cash + qty * current.low().doubleValue() * (1 - slippage) * (1 - fee);
                drawdown = Math.max(drawdown, 100 * (peak - intrabarEquity) / peak);
            }
        }
        // Unrealized open position is marked to market, not counted as completed trade.
        double finalClose = candles.get(candles.size()-1).close().doubleValue();
        double finalEquity = cash + qty * finalClose * (1 - slippage) * (1 - fee);
        double bh = 100 * (candles.get(candles.size()-1).close().doubleValue() /
            candles.get(0).close().doubleValue() - 1);
        return new Report(strategy.name(), candles.size(), initial, finalEquity,
            100 * (finalEquity / initial - 1), bh, trades.size(),
            trades.isEmpty() ? 0 : 100 * wins / trades.size(),
            losses == 0 ? (gains > 0 ? 999999.0 : 0) : gains / losses,
            drawdown, fees, List.copyOf(trades),
            "Historical simulation; no guarantee. One position, next-open fills, stop-first ambiguity.");
    }

    private static boolean bullish(List<Candle> c, int end, int fast, int slow) {
        double f = ema(c, end, fast), s = ema(c, end, slow);
        double prevF = ema(c, end-1, fast), prevS = ema(c, end-1, slow);
        return f > s && prevF <= prevS && c.get(end).close().doubleValue() > f;
    }
    private static double ema(List<Candle> c, int end, int period) {
        double alpha = 2.0 / (period + 1);
        double result = c.get(0).close().doubleValue();
        for (int i = 1; i <= end; i++) result = alpha * c.get(i).close().doubleValue() + (1-alpha)*result;
        return result;
    }
    private static double atr(List<Candle> c, int end, int period) {
        double total = 0;
        for (int i = end-period+1; i <= end; i++) {
            double high = c.get(i).high().doubleValue(), low = c.get(i).low().doubleValue();
            double prev = c.get(i-1).close().doubleValue();
            total += Math.max(high-low, Math.max(Math.abs(high-prev), Math.abs(low-prev)));
        }
        return total / period;
    }
}
