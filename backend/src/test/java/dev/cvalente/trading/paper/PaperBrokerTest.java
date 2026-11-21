package dev.cvalente.trading.paper;

import dev.cvalente.trading.domain.Candle;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.cvalente.trading.persistence.*;
import java.util.List;

class PaperBrokerTest {
    @Test void marketBuyAndSellTrackCashPositionAndPnl() {
        var broker = new PaperBroker(bd(1000), bd("0.001"));
        var buy = broker.submit("BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(2), null, Instant.EPOCH);
        broker.onCandle(candle(100, 0));
        assertEquals(2, broker.snapshot().positions().get(0).quantity().doubleValue(), 1e-9);
        broker.submit("BTCUSDT", OrderSide.SELL, OrderType.MARKET, bd(2), null, Instant.ofEpochSecond(60));
        broker.onCandle(candle(110, 60));
        assertEquals(19.58, broker.snapshot().realizedPnl().doubleValue(), 1e-9);
        assertEquals(1019.58, broker.snapshot().cash().doubleValue(), 1e-9);
    }

    @Test void limitOrderFillsOnlyWhenCandleTouchesPrice() {
        var broker = new PaperBroker(bd(1000), bd("0"));
        var order = broker.submit("BTCUSDT", OrderSide.BUY, OrderType.LIMIT, bd(1), bd(95), Instant.EPOCH);
        assertTrue(broker.onCandle(candle(100, 0)).isEmpty());
        assertEquals(1, broker.onCandle(candle(100, 60, 94)).size());
        assertEquals("FILLED", broker.snapshot().fills().isEmpty() ? "" : broker.snapshot().openOrders().stream().findFirst().map(PaperOrder::status).orElse("FILLED"));
    }

    @Test void cancelRemovesOrderFromOpenBook() {
        var broker = new PaperBroker();
        var order = broker.submit("BTCUSDT", OrderSide.BUY, OrderType.LIMIT, bd(1), bd(90), Instant.EPOCH);
        broker.cancel(order.id());
        assertTrue(broker.snapshot().openOrders().isEmpty());
    }

    @Test void notionalLimitsAreAppliedToLimitAndMarketFills() {
        var broker = new PaperBroker(bd(1000), bd("0"), bd(100), bd(200));
        assertThrows(IllegalArgumentException.class, () ->
            broker.submit("BTCUSDT", OrderSide.BUY, OrderType.LIMIT, bd(1), bd(50), Instant.EPOCH));
        broker.submit("BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(3), null, Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, () -> broker.onCandle(candle(100, 0)));
    }

    @Test void availableCashIsCheckedAtMarketFill() {
        var broker = new PaperBroker(bd(100), bd("0"));
        broker.submit("BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(2), null, Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, () -> broker.onCandle(candle(60, 0)));
    }

    @Test void persistedStateIsRestoredOnBrokerConstruction() {
        var orders = mock(PaperOrderRepository.class); var fills = mock(PaperFillRepository.class);
        var positions = mock(PaperPositionRepository.class); var accounts = mock(PaperAccountRepository.class);
        var account = new PaperAccountEntity(); account.id = 1L; account.cash = bd(750);
        var order = new PaperOrderEntity(); order.id = 4L; order.symbol = "BTCUSDT"; order.side = "BUY"; order.type = "LIMIT";
        order.quantity = bd(1); order.limitPrice = bd(100); order.createdAt = Instant.EPOCH; order.status = "OPEN";
        when(accounts.findById(1L)).thenReturn(java.util.Optional.of(account));
        when(orders.findAll()).thenReturn(List.of(order)); when(fills.findAll()).thenReturn(List.of()); when(positions.findAll()).thenReturn(List.of());
        var broker = new PaperBroker(orders, fills, positions, accounts);
        assertEquals(750, broker.snapshot().cash().doubleValue(), 1e-9);
        assertEquals(1, broker.snapshot().openOrders().size());
        assertEquals(5, broker.submit("BTCUSDT", OrderSide.BUY, OrderType.LIMIT, bd(1), bd(90), Instant.EPOCH).id());
    }

    private static Candle candle(int close, long seconds) { return candle(close, seconds, close - 1); }
    private static Candle candle(int close, long seconds, int low) {
        BigDecimal p = bd(close);
        return new Candle(Instant.ofEpochSecond(seconds), p, p.add(BigDecimal.ONE), bd(low), p, bd(10), p.multiply(bd(10)));
    }
    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
    private static BigDecimal bd(String value) { return new BigDecimal(value); }
}
