package dev.cvalente.trading.paper;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final BigDecimal minNotional;
    private final BigDecimal maxNotional;
    private final PaperOrderRepository orderRepository;
    private final PaperFillRepository fillRepository;
    private final PaperPositionRepository positionRepository;
    private BigDecimal cash;
    private long nextId = 1;

    public PaperBroker() { this(new BigDecimal("10000"), new BigDecimal("0.001"), BigDecimal.ZERO, new BigDecimal("1E+1000"), null, null, null); }
    public PaperBroker(BigDecimal initialCash, BigDecimal feeRate) {
        this(initialCash, feeRate, BigDecimal.ZERO, new BigDecimal("1E+1000"), null, null, null);
    }
    public PaperBroker(BigDecimal initialCash, BigDecimal feeRate, BigDecimal minNotional, BigDecimal maxNotional) {
        this(initialCash, feeRate, minNotional, maxNotional, null, null, null);
    }
    @Autowired
    public PaperBroker(PaperOrderRepository orderRepository, PaperFillRepository fillRepository, PaperPositionRepository positionRepository) {
        this(new BigDecimal("10000"), new BigDecimal("0.001"), BigDecimal.ZERO, new BigDecimal("1E+1000"), orderRepository, fillRepository, positionRepository);
    }
    private PaperBroker(BigDecimal initialCash, BigDecimal feeRate, BigDecimal minNotional, BigDecimal maxNotional,
                        PaperOrderRepository orderRepository, PaperFillRepository fillRepository, PaperPositionRepository positionRepository) {
        if (initialCash == null || initialCash.signum() <= 0 || feeRate == null || feeRate.signum() < 0 || feeRate.compareTo(BigDecimal.ONE) >= 0 ||
            minNotional == null || minNotional.signum() < 0 || maxNotional == null || maxNotional.compareTo(minNotional) < 0)
            throw new IllegalArgumentException("Invalid paper broker configuration");
        this.cash = initialCash; this.feeRate = feeRate; this.minNotional = minNotional; this.maxNotional = maxNotional;
        this.orderRepository = orderRepository; this.fillRepository = fillRepository; this.positionRepository = positionRepository;
    }

    public synchronized PaperOrder submit(String symbol, OrderSide side, OrderType type, BigDecimal quantity,
                                          BigDecimal limitPrice, Instant now) {
        if (symbol == null || symbol.isBlank() || side == null || type == null || quantity == null || quantity.signum() <= 0 || now == null ||
            (type == OrderType.LIMIT && (limitPrice == null || limitPrice.signum() <= 0)))
            throw new IllegalArgumentException("Invalid paper order");
        if (type == OrderType.LIMIT) validateNotional(quantity.multiply(limitPrice));
        PaperOrder order = new PaperOrder(nextId++, symbol, side, type, quantity, limitPrice, now, "OPEN");
        orders.put(order.id(), order);
        persist(order);
        return order;
    }

    public synchronized void cancel(long id) {
        PaperOrder order = orders.get(id);
        if (order == null || !order.status().equals("OPEN")) throw new IllegalArgumentException("Order is not open: " + id);
        orders.put(id, new PaperOrder(order.id(), order.symbol(), order.side(), order.type(), order.quantity(), order.limitPrice(), order.createdAt(), "CANCELED"));
        persist(orders.get(id));
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
                persist(orders.get(order.id())); persist(fill);
            }
        }
        for (MutablePosition position : positions.values()) position.markPrice = candle.close();
        persistPositions();
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
        validateNotional(gross);
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

    private void validateNotional(BigDecimal notional) {
        if (notional.compareTo(minNotional) < 0 || notional.compareTo(maxNotional) > 0)
            throw new IllegalArgumentException("Order notional outside configured limits");
    }

    private void persist(PaperOrder order) {
        if (orderRepository == null) return;
        PaperOrderEntity entity = new PaperOrderEntity(); entity.id = order.id(); entity.symbol = order.symbol();
        entity.side = order.side().name(); entity.type = order.type().name(); entity.quantity = order.quantity();
        entity.limitPrice = order.limitPrice(); entity.createdAt = order.createdAt(); entity.status = order.status(); orderRepository.save(entity);
    }
    private void persist(PaperFill fill) {
        if (fillRepository == null) return;
        PaperFillEntity entity = new PaperFillEntity(); entity.orderId = fill.orderId(); entity.symbol = fill.symbol();
        entity.side = fill.side().name(); entity.quantity = fill.quantity(); entity.price = fill.price(); entity.fee = fill.fee(); entity.fillTime = fill.time(); fillRepository.save(entity);
    }
    private void persistPositions() {
        if (positionRepository == null) return;
        for (MutablePosition position : positions.values()) {
            PaperPositionEntity entity = new PaperPositionEntity(); entity.symbol = position.symbol; entity.quantity = position.quantity;
            entity.averageEntry = position.average; entity.markPrice = position.markPrice; entity.realizedPnl = position.realized; positionRepository.save(entity);
        }
    }

    private static final class MutablePosition {
        final String symbol; BigDecimal quantity = BigDecimal.ZERO, average = BigDecimal.ZERO, markPrice = BigDecimal.ZERO, realized = BigDecimal.ZERO;
        MutablePosition(String symbol) { this.symbol = symbol; }
        void buy(BigDecimal q, BigDecimal price, BigDecimal fee) { average = average.multiply(quantity).add(price.multiply(q)).divide(quantity.add(q), 12, RoundingMode.HALF_UP); quantity = quantity.add(q); markPrice = price; realized = realized.subtract(fee); }
        void sell(BigDecimal q, BigDecimal price, BigDecimal fee) { realized = realized.add(price.subtract(average).multiply(q).subtract(fee)); quantity = quantity.subtract(q); markPrice = price; if (quantity.signum() == 0) average = BigDecimal.ZERO; }
        PaperPosition snapshot() { return new PaperPosition(symbol, quantity, average, markPrice, markPrice.subtract(average).multiply(quantity), realized); }
    }
}
