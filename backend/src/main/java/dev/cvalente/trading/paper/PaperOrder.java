package dev.cvalente.trading.paper;

import java.math.BigDecimal;
import java.time.Instant;

public record PaperOrder(long id, String symbol, OrderSide side, OrderType type,
                         BigDecimal quantity, BigDecimal limitPrice, Instant createdAt,
                         String status) {}
