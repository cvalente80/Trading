package dev.cvalente.trading.paper;

import java.math.BigDecimal;
import java.time.Instant;

public record PaperFill(long orderId, String symbol, OrderSide side, BigDecimal quantity,
                        BigDecimal price, BigDecimal fee, Instant time) {}
