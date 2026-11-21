package dev.cvalente.trading.paper;

import java.math.BigDecimal;

public record PaperPosition(String symbol, BigDecimal quantity, BigDecimal averageEntry,
                            BigDecimal markPrice, BigDecimal unrealizedPnl, BigDecimal realizedPnl) {}
