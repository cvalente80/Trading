package dev.cvalente.trading.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Candle(
    Instant openTime, BigDecimal open, BigDecimal high, BigDecimal low,
    BigDecimal close, BigDecimal volume, BigDecimal quoteVolume
) {}
