package dev.cvalente.trading.paper;

import dev.cvalente.trading.domain.Candle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/** In-memory deterministic spot paper broker. One account, no real order routing. */
@Service
public class PaperBroker {
    private final Map<Long, PaperOrder> orders = new LinkedHashMap<>();
    private final Map<String, MutablePosition> positions = new LinkedHashMap<>();
    private final List<PaperFill> fills = new ArrayList<>();
    private final BigDecimal feeRate;
    private BigDecimal cash;
    private long nextId = 1;

    public PaperBroker() { this(new BigDecimal("10000"), new BigDecimal("0.001")); }
    public PaperBroker(BigDecimal initialCash, BigDecimal feeRate) {
        if (initialCash == null || initialCash.signum() <= 0 || feeRate == null || feeRate.signum() < 0 || feeRate.compareTo(BigDecimal.ONE) >= 0)
            throw new IllegalArgumentException("Invalid paper broker configuration");
        this.cash = initialCash; this.feeRate = feeRate;
    }

    public synchronized PaperOrder submit(String symbol, OrderSide side, OrderType type, BigDecimal quantity,
                                          BigDecimal limitPrice, Instant now) {
        if (symbol == null || symbol.isBlank() || side == null || type == null || quantity == null || quantity.signum() <= 0 || now == null ||
            (type == OrderType.LIMIT && (limitPrice == null || limitPrice.signum() <= 0)))
            throw new IllegalArgumentException("Invalid paper order");
        PaperOrder order = new PaperOrder(nextId++, symbol, side, type, quantity, limitPrice, now, "OPEN");
        orders.put(order.id(), order);
        return order;
    }

    public synchronized void cancel(long id) {
        PaperOrder order = orders.get(id);
        if (order == null || !order.status().equals("OPEN")) throw new IllegalArgumentException("Order is not open: " + id);
        orders.put(id, new PaperOrder(order.id(), order.symbol(), order.side(), order.type(), order.quantity(), order.limitPrice(), order.createdAt(), "CANCELED"));
    }

    public synchronized List<PaperFill> onCandle(Candle candle) {
        List<PaperFill> created = new ArrayList<>();
        for (PaperOrder order : List.copyOf(orders.values())) {
            if (!order.status().equals("OPEN")) continue;
            BigDecimal price = order.type() == OrderType.MARKET ? candle.open() : limitFill(order, candle);
            if (price != null) {
                PaperFill fill = execute(order, price, candle.openTime());
                created.add(fill); fills.add(fill);
                orders.put(order.id(), new PaperOrder(order.id(), order.symbol(), order.side(), order.type(), order.quantity(), order.limitPrice(), order.createdAt(), "FILLED"));
            }
        }
        for (MutablePosition position : positions.values()) position.markPrice = candle.close();
        return List.copyOf(created);
    }

    public synchronized PaperAccount snapshot() {
        List<PaperPosition> result = positions.values().stream().map(MutablePosition::snapshot).toList();
        BigDecimal unrealized = result.stream().map(PaperPosition::unrealizedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal equity = cash.add(result.stream().map(p -> p.quantity().multiply(p.markPrice())).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal realized = result.stream().map(PaperPosition::realizedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaperAccount(cash, equity, realized, unrealized, result,
            orders.values().stream().filter(o -> o.status().equals("OPEN")).toList(), List.copyOf(fills));
    }

    private BigDecimal limitFill(PaperOrder order, Candle candle) {
        if (order.side() == OrderSide.BUY && candle.low().compareTo(order.limitPrice()) <= 0) return order.limitPrice();
        if (order.side() == OrderSide.SELL && candle.high().compareTo(order.limitPrice()) >= 0) return order.limitPrice();
        return null;
    }

    private PaperFill execute(PaperOrder order, BigDecimal price, Instant time) {
        BigDecimal gross = price.multiply(order.quantity()), fee = gross.multiply(feeRate);
        MutablePosition position = positions.computeIfAbsent(order.symbol(), MutablePosition::new);
        if (order.side() == OrderSide.BUY) {
            BigDecimal total = gross.add(fee);
            if (cash.compareTo(total) < 0) throw new IllegalArgumentException("Insufficient cash");
            cash = cash.subtract(total); position.buy(order.quantity(), price, fee);
        } else {
            if (position.quantity.compareTo(order.quantity()) < 0) throw new IllegalArgumentException("Insufficient position");
            cash = cash.add(gross.subtract(fee)); position.sell(order.quantity(), price, fee);
        }
        return new PaperFill(order.id(), order.symbol(), order.side(), order.quantity(), price, fee, time);
    }

    private static final class MutablePosition {
        final String symbol; BigDecimal quantity = BigDecimal.ZERO, average = BigDecimal.ZERO, markPrice = BigDecimal.ZERO, realized = BigDecimal.ZERO;
        MutablePosition(String symbol) { this.symbol = symbol; }
        void buy(BigDecimal q, BigDecimal price, BigDecimal fee) { average = average.multiply(quantity).add(price.multiply(q)).divide(quantity.add(q), 12, RoundingMode.HALF_UP); quantity = quantity.add(q); markPrice = price; realized = realized.subtract(fee); }
        void sell(BigDecimal q, BigDecimal price, BigDecimal fee) { realized = realized.add(price.subtract(average).multiply(q).subtract(fee)); quantity = quantity.subtract(q); markPrice = price; if (quantity.signum() == 0) average = BigDecimal.ZERO; }
        PaperPosition snapshot() { return new PaperPosition(symbol, quantity, average, markPrice, markPrice.subtract(average).multiply(quantity), realized); }
    }
}
