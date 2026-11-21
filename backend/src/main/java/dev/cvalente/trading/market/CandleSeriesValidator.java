package dev.cvalente.trading.market;

import dev.cvalente.trading.domain.Candle;
import java.time.Instant;
import java.util.List;

/** Validates chronological completeness of a candle series. */
public final class CandleSeriesValidator {
    private CandleSeriesValidator() {}

    public static void validate(List<Candle> candles, long intervalMillis) {
        if (candles == null || intervalMillis <= 0) throw new IllegalArgumentException("Invalid candle series or interval");
        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            if (candle == null || candle.openTime() == null) throw new IllegalArgumentException("Missing candle at index " + i);
            if (i > 0 && candle.openTime().toEpochMilli() - candles.get(i - 1).openTime().toEpochMilli() != intervalMillis)
                throw new IllegalArgumentException("Candle gap or overlap at index " + i);
        }
    }

    public static boolean isComplete(List<Candle> candles, Instant firstOpen,
                                     long intervalMillis, int expectedCount) {
        if (candles == null || firstOpen == null || expectedCount < 0 || candles.size() != expectedCount) return false;
        try { validate(candles, intervalMillis); }
        catch (IllegalArgumentException ex) { return false; }
        return expectedCount == 0 || candles.get(0).openTime().equals(firstOpen);
    }
}
