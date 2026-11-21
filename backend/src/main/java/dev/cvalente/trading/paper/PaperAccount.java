package dev.cvalente.trading.paper;

import java.math.BigDecimal;
import java.util.List;

public record PaperAccount(BigDecimal cash, BigDecimal equity, BigDecimal realizedPnl,
                           BigDecimal unrealizedPnl, List<PaperPosition> positions,
                           List<PaperOrder> openOrders, List<PaperFill> fills) {}
