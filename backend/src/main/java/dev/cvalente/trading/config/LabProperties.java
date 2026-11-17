package dev.cvalente.trading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "lab")
public record LabProperties(Portfolio portfolio, Trading trading) {
    public record Portfolio(BigDecimal initialBalanceUsdt, BigDecimal riskPerTradePercent) {}
    public record Trading(String mode, String market, boolean realOrdersEnabled) {}
}
