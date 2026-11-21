package dev.cvalente.trading.api;

import dev.cvalente.trading.paper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaperControllerIntegrationTest {
    MockMvc mvc; PaperBroker broker; DecisionJournal journal; PaperSignalService signals;
    @BeforeEach void setUp() {
        broker = mock(PaperBroker.class); journal = mock(DecisionJournal.class);
        signals = mock(PaperSignalService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PaperController(broker, journal, signals)).build();
    }
    @Test void accountEndpointReturnsCashAndEquity() throws Exception {
        when(broker.snapshot()).thenReturn(new PaperAccount(bd(1000), bd(1000), BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of()));
        mvc.perform(get("/api/v1/paper/account")).andExpect(status().isOk()).andExpect(jsonPath("$.cash").value(1000));
    }
    @Test void orderEndpointCreatesMarketOrder() throws Exception {
        when(broker.submit(eq("BTCUSDT"), eq(OrderSide.BUY), eq(OrderType.MARKET), eq(bd(1)), isNull(), any()))
            .thenReturn(new PaperOrder(1, "BTCUSDT", OrderSide.BUY, OrderType.MARKET, bd(1), null, Instant.EPOCH, "OPEN"));
        mvc.perform(post("/api/v1/paper/orders").contentType("application/json")
                .content("{\"symbol\":\"BTCUSDT\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }
    @Test void decisionAndCandleEndpointsAreExposed() throws Exception {
        when(journal.append(any(), eq("BTCUSDT"), eq("DAY"), eq("BUY"), eq(80), any()))
            .thenReturn(new DecisionJournal.Entry(2L, Instant.EPOCH, "BTCUSDT", "DAY", "BUY", 80, "EMA"));
        when(broker.onCandle(any())).thenReturn(List.of());
        when(broker.snapshot()).thenReturn(new PaperAccount(bd(1000), bd(1000), BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of()));
        mvc.perform(post("/api/v1/paper/decisions").contentType("application/json")
                .content("{\"symbol\":\"BTCUSDT\",\"strategy\":\"DAY\",\"action\":\"BUY\",\"score\":80,\"reasons\":[\"EMA\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2));
        mvc.perform(post("/api/v1/paper/candles").contentType("application/json")
                .content("{\"openTime\":\"1970-01-01T00:00:00Z\",\"open\":100,\"high\":101,\"low\":99,\"close\":100,\"volume\":10,\"quoteVolume\":1000}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.account.cash").value(1000));
    }
    @Test void signalEndpointExposesEvaluationResult() throws Exception {
        var evaluation = new PaperSignalService.Evaluation(
            new dev.cvalente.trading.domain.Signal("BTCUSDT", dev.cvalente.trading.domain.StrategyType.DAY,
                "WATCH", 50, null, null, null, null, Instant.EPOCH, List.of("RSI")), null, null);
        when(signals.evaluate(eq("BTCUSDT"), eq(dev.cvalente.trading.domain.StrategyType.DAY), anyList())).thenReturn(evaluation);
        mvc.perform(post("/api/v1/paper/signals/evaluate").contentType("application/json")
                .content("{\"symbol\":\"BTCUSDT\",\"strategy\":\"DAY\",\"candles\":[]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.signal.action").value("WATCH"));
    }
    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
}
