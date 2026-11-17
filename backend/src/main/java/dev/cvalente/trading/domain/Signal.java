package dev.cvalente.trading.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Signal(
    String symbol,
    StrategyType strategy,
    String action,
    int score,
    BigDecimal referencePrice,
    BigDecimal suggestedStop,
    BigDecimal suggestedTarget,
    BigDecimal riskReward,
    Instant generatedAt,
    List<String> reasons
) {}
