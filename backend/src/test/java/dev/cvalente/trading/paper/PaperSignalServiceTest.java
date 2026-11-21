package dev.cvalente.trading.paper;

import dev.cvalente.trading.domain.*;
import dev.cvalente.trading.strategy.SignalEngine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaperSignalServiceTest {
    @Test void buySignalIsJournaledAndCreatesMarketOrder() {
        var engine = mock(SignalEngine.class); var broker = mock(PaperBroker.class); var journal = mock(DecisionJournal.class);
        var signal = new Signal("BTCUSDT", StrategyType.DAY, "BUY_CANDIDATE", 80, bd(100), bd(98), bd(106), bd(3), Instant.EPOCH, List.of("EMA"));
        when(engine.evaluate(eq("BTCUSDT"), eq(StrategyType.DAY), anyList())).thenReturn(signal);
        when(journal.append(any(), anyString(), anyString(), anyString(), anyInt(), anyList())).thenReturn(
            new DecisionJournal.Entry(1L, Instant.EPOCH, "BTCUSDT", "DAY", "BUY_CANDIDATE", 80, "EMA"));
        when(broker.snapshot()).thenReturn(new PaperAccount(bd(10000), bd(10000), BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of()));
        when(broker.submit(anyString(), eq(OrderSide.BUY), eq(OrderType.MARKET), any(), isNull(), any()))
            .thenReturn(new PaperOrder(1, "BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(1), null, Instant.EPOCH, "OPEN"));
        var result = new PaperSignalService(engine, broker, journal).evaluate("BTCUSDT", StrategyType.DAY, List.of());
        assertEquals("BUY_CANDIDATE", result.signal().action());
        verify(journal).append(any(), eq("BTCUSDT"), eq("DAY"), eq("BUY_CANDIDATE"), eq(80), anyList());
        verify(broker).submit(anyString(), eq(OrderSide.BUY), eq(OrderType.MARKET), any(), isNull(), any());
    }
    @Test void watchSignalIsJournaledWithoutOrder() {
        var engine = mock(SignalEngine.class); var broker = mock(PaperBroker.class); var journal = mock(DecisionJournal.class);
        var signal = new Signal("BTCUSDT", StrategyType.DAY, "WATCH", 50, bd(100), bd(98), bd(106), bd(3), Instant.EPOCH, List.of("RSI"));
        when(engine.evaluate(anyString(), any(), anyList())).thenReturn(signal);
        when(journal.append(any(), anyString(), anyString(), anyString(), anyInt(), anyList())).thenReturn(null);
        var result = new PaperSignalService(engine, broker, journal).evaluate("BTCUSDT", StrategyType.DAY, List.of());
        assertEquals(null, result.order()); verify(broker, never()).submit(any(), any(), any(), any(), any(), any());
    }
    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
}
