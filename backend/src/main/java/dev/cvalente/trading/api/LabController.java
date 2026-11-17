package dev.cvalente.trading.api;

import dev.cvalente.trading.config.LabProperties;
import dev.cvalente.trading.domain.StrategyType;
import dev.cvalente.trading.market.BinancePublicMarketClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5174")
public class LabController {
    private final LabProperties properties;
    private final BinancePublicMarketClient market;

    public LabController(LabProperties properties, BinancePublicMarketClient market) {
        this.properties = properties;
        this.market = market;
    }

    @GetMapping("/lab/config")
    public Map<String, Object> config() {
        return Map.of("initialBalanceUsdt", properties.portfolio().initialBalanceUsdt(),
            "riskPerTradePercent", properties.portfolio().riskPerTradePercent(),
            "mode", properties.trading().mode(), "market", properties.trading().market(),
            "realOrdersEnabled", properties.trading().realOrdersEnabled(),
            "strategies", List.of(StrategyType.DAY, StrategyType.SWING));
    }

    @GetMapping("/market/universe")
    public List<BinancePublicMarketClient.Ticker24h> universe() {
        return market.tickers24h().stream()
            .filter(t -> t.symbol() != null && t.symbol().endsWith("USDT"))
            .filter(t -> !t.symbol().contains("UPUSDT") && !t.symbol().contains("DOWNUSDT"))
            .filter(t -> parse(t.quoteVolume()) >= 5_000_000d)
            .sorted((a,b) -> Double.compare(parse(b.quoteVolume()), parse(a.quoteVolume())))
            .limit(50).toList();
    }

    private static double parse(String value) {
        try { return Double.parseDouble(value); } catch (Exception ex) { return 0d; }
    }
}
