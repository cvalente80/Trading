package dev.cvalente.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TradingLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradingLabApplication.class, args);
    }
}
