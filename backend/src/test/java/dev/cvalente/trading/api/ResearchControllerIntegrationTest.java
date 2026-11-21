package dev.cvalente.trading.api;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.market.CandleHistoryService;
import dev.cvalente.trading.research.BacktestHistoryService;
import dev.cvalente.trading.research.ReplayBacktester;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResearchControllerIntegrationTest {
    MockMvc mvc;
    CandleHistoryService candles;
    BacktestHistoryService history;

    @BeforeEach void setUp() {
        candles = mock(CandleHistoryService.class);
        history = mock(BacktestHistoryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ResearchController(candles, history))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test void backtestEndpointAcceptsResearchConfigurationAndReturnsMetrics() throws Exception {
        when(candles.load("BTCUSDT", "15m", 100)).thenReturn(new CandleHistoryService.History(flatCandles(100), 0, 100, true));
        when(history.save(eq("BTCUSDT"), eq("15m"), eq(.001), eq(.0005), any())).thenReturn(17L);

        mvc.perform(get("/api/v1/research/backtest")
                .param("symbol", "BTCUSDT").param("strategy", "DAY").param("candles", "100")
                .param("riskPercent", ".5").param("fee", ".001").param("slippage", ".0005")
                .param("higherTimeframeTrend", "true").param("adxFilter", "true")
                .param("rsiFilter", "true").param("volumeFilter", "true")
                .param("maxEmaDistancePercent", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.report.expectancy").value(0))
            .andExpect(jsonPath("$.report.randomBenchmarkPercent").exists());
        verify(history).save(eq("BTCUSDT"), eq("15m"), eq(.001), eq(.0005), any());
    }

    @Test void incompleteHistoryIsReturnedAsBadRequest() throws Exception {
        when(candles.load("ETHUSDT", "4h", 100)).thenReturn(new CandleHistoryService.History(List.of(), 0, 0, false));
        mvc.perform(get("/api/v1/research/backtest").param("symbol", "ETHUSDT")
                .param("strategy", "SWING").param("candles", "100"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Cannot backtest incomplete candle history"));
    }

    @Test void historyEndpointsReturnSavedRunsAndDetails() throws Exception {
        var report = ReplayBacktester.run(flatCandles(100), dev.cvalente.trading.domain.StrategyType.DAY,
            10_000, .5, .001, .0005);
        var summary = new BacktestHistoryService.RunSummary(17L, Instant.EPOCH, "BTCUSDT", "DAY", "15m", 100,
            .001, .0005, 10_000, 10_000, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0);
        when(history.recent()).thenReturn(List.of(summary));
        when(history.detail(17L)).thenReturn(new BacktestHistoryService.RunDetails(summary, report.trades(), report.note()));
        mvc.perform(get("/api/v1/research/backtests")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(17));
        mvc.perform(get("/api/v1/research/backtests/17")).andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.expectancy").value(0));
    }

    private static List<Candle> flatCandles(int count) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal p = BigDecimal.valueOf(100);
            result.add(new Candle(Instant.ofEpochSecond(i * 900L), p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p,
                BigDecimal.TEN, p.multiply(BigDecimal.TEN)));
        }
        return result;
    }
}
