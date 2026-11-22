package dev.cvalente.trading.paper;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Requires Docker; run with -Dtest=PaperBrokerPostgresIT to target it explicitly. */
@Testcontainers(disabledWithoutDocker = true)
class PaperBrokerPostgresIT extends PersistenceIntegrationTestBase {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("paper_test").withUsername("test").withPassword("test");

    @Test void persistsAndRestoresAccountOrdersFillsAndPositions() {
        var first = new PaperBroker(orderRepository, fillRepository, positionRepository, accountRepository);
        first.submit("BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(2), null, Instant.EPOCH);
        first.onCandle(candle(100, 0));
        first.submit("BTCUSDT", OrderSide.SELL, OrderType.MARKET, bd(1), null, Instant.ofEpochSecond(60));
        first.onCandle(candle(110, 60));

        var restarted = new PaperBroker(orderRepository, fillRepository, positionRepository, accountRepository);
        assertEquals(1, restarted.snapshot().openOrders().size());
        assertEquals(1, restarted.snapshot().fills().size());
        assertEquals(1, restarted.snapshot().positions().get(0).quantity().intValue());
        assertEquals(first.snapshot().cash(), restarted.snapshot().cash());
    }

    private static Candle candle(int close, long seconds) {
        BigDecimal p = bd(close);
        return new Candle(Instant.ofEpochSecond(seconds), p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p, bd(10), p.multiply(bd(10)));
    }
    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
}
