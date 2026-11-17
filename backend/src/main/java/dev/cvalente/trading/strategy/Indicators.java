package dev.cvalente.trading.strategy;

import dev.cvalente.trading.domain.Candle;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public final class Indicators {
    private static final MathContext MC = MathContext.DECIMAL64;
    private Indicators() {}

    public static BigDecimal ema(List<Candle> candles, int period) {
        if (candles.size() < period || period < 2) return null;
        BigDecimal alpha = BigDecimal.valueOf(2.0 / (period + 1.0));
        BigDecimal value = candles.get(0).close();
        for (int i = 1; i < candles.size(); i++)
            value = candles.get(i).close().multiply(alpha, MC)
                .add(value.multiply(BigDecimal.ONE.subtract(alpha), MC), MC);
        return value;
    }

    public static BigDecimal rsi(List<Candle> candles, int period) {
        if (candles.size() <= period) return null;
        BigDecimal gains = BigDecimal.ZERO, losses = BigDecimal.ZERO;
        for (int i = candles.size() - period; i < candles.size(); i++) {
            BigDecimal change = candles.get(i).close().subtract(candles.get(i - 1).close());
            if (change.signum() > 0) gains = gains.add(change);
            else losses = losses.add(change.abs());
        }
        if (losses.signum() == 0) return BigDecimal.valueOf(100);
        BigDecimal rs = gains.divide(losses, MC);
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100)
            .divide(BigDecimal.ONE.add(rs), MC));
    }

    public static BigDecimal averageQuoteVolume(List<Candle> candles, int period) {
        if (candles.size() < period) return null;
        return candles.subList(candles.size() - period, candles.size()).stream()
            .map(Candle::quoteVolume).reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(period), MC);
    }
}
