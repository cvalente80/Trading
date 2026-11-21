package dev.cvalente.trading.strategy;

import dev.cvalente.trading.domain.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SignalEngine {
    public Signal evaluate(String symbol, StrategyType strategy, List<Candle> candles) {
        int fast = CrossoverSignalPolicy.fastPeriod(strategy);
        int slow = CrossoverSignalPolicy.slowPeriod(strategy);
        int rsiPeriod = 14;
        if (candles == null || candles.size() < slow + 2)
            return new Signal(symbol, strategy, "INSUFFICIENT_DATA", 0, null, null, null, null, Instant.now(),
                List.of("Dados insuficientes para calcular os indicadores"));

        BigDecimal close = candles.get(candles.size() - 1).close();
        BigDecimal fastEma = Indicators.ema(candles, fast);
        BigDecimal slowEma = Indicators.ema(candles, slow);
        BigDecimal rsi = Indicators.rsi(candles, rsiPeriod);
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (fastEma.compareTo(slowEma) > 0) { score += 35; reasons.add("EMA rápida acima da EMA lenta: tendência positiva"); }
        else reasons.add("EMA rápida não confirma tendência positiva");
        if (close.compareTo(fastEma) > 0) { score += 20; reasons.add("Fecho acima da EMA rápida"); }
        if (rsi.compareTo(BigDecimal.valueOf(45)) >= 0 && rsi.compareTo(BigDecimal.valueOf(68)) <= 0) {
            score += 25; reasons.add("RSI em zona de momentum sem sobrecompra extrema");
        } else reasons.add("RSI fora da zona de momentum definida");
        BigDecimal avgVolume = Indicators.averageQuoteVolume(candles, Math.min(20, candles.size()));
        BigDecimal lastVolume = candles.get(candles.size() - 1).quoteVolume();
        if (avgVolume != null && lastVolume.compareTo(avgVolume) > 0) {
            score += 20; reasons.add("Volume da última vela acima da média");
        }

        // Stop/target de demonstração: 1.5% stop e 3% target. A versão seguinte
        // deverá substituir isto por ATR, estrutura de mercado e validação histórica.
        BigDecimal stop = close.multiply(new BigDecimal("0.985")).setScale(8, RoundingMode.HALF_UP);
        BigDecimal target = close.multiply(new BigDecimal("1.03")).setScale(8, RoundingMode.HALF_UP);
        BigDecimal rr = target.subtract(close).divide(close.subtract(stop), 4, RoundingMode.HALF_UP);
        CrossoverSignalPolicy.Decision decision = CrossoverSignalPolicy.latest(candles, strategy);
        String action = decision == CrossoverSignalPolicy.Decision.BUY ? "BUY_CANDIDATE" :
            decision == CrossoverSignalPolicy.Decision.EXIT ? "BEARISH_EXIT" :
            score >= 50 ? "WATCH" : "NO_TRADE";
        return new Signal(symbol, strategy, action, score, close, stop, target, rr, Instant.now(), List.copyOf(reasons));
    }
}
