package dev.cvalente.trading.research;

import java.util.Optional;

/** Deterministic long-only OHLC exit policy used by the replay backtester. */
public final class ExecutionModel {
    private ExecutionModel() {}

    public record Exit(String reason, double price) {}

    /**
     * Evaluates an already open position. A stop is always checked before a target
     * because OHLC data does not reveal the intrabar path.
     */
    public static Optional<Exit> existingPosition(double open, double low, double high,
                                                   double stop, double target, double slippage) {
        validatePrices(open, low, high, stop, target);
        validateSlippage(slippage);
        if (open <= stop) return Optional.of(new Exit("STOP_GAP", open * (1 - slippage)));
        if (low <= stop) return Optional.of(new Exit("STOP", stop * (1 - slippage)));
        if (high >= target) return Optional.of(new Exit("TARGET", target * (1 - slippage)));
        return Optional.empty();
    }

    /** Evaluates a stop on the candle where the position was entered. */
    public static Optional<Exit> entryBar(double open, double low, double stop,
                                          double slippage) {
        if (!Double.isFinite(open) || !Double.isFinite(low) || !Double.isFinite(stop) ||
            open <= 0 || low <= 0 || stop <= 0 || low > open) {
            throw new IllegalArgumentException("Invalid entry-bar execution prices");
        }
        validateSlippage(slippage);
        if (low > stop) return Optional.empty();
        double reference = open <= stop ? open : stop;
        return Optional.of(new Exit("ENTRY_BAR_STOP", reference * (1 - slippage)));
    }

    private static void validatePrices(double open, double low, double high,
                                       double stop, double target) {
        if (!Double.isFinite(open) || !Double.isFinite(low) || !Double.isFinite(high) ||
            !Double.isFinite(stop) || !Double.isFinite(target) || open <= 0 || low <= 0 ||
            high < low || low > open || high < open || stop <= 0 || target <= 0) {
            throw new IllegalArgumentException("Invalid OHLC execution prices");
        }
    }

    private static void validateSlippage(double slippage) {
        if (!Double.isFinite(slippage) || slippage < 0 || slippage >= 1) {
            throw new IllegalArgumentException("Invalid execution slippage");
        }
    }
}
