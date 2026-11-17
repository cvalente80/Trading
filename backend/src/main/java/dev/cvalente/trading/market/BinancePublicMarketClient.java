package dev.cvalente.trading.market;

import dev.cvalente.trading.domain.Candle;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class BinancePublicMarketClient {
    private static final Set<String> INTERVALS = Set.of("5m", "15m", "1h", "4h", "1d");
    private final RestClient client = RestClient.builder().baseUrl("https://api.binance.com").build();

    public List<Ticker24h> tickers24h() {
        Ticker24h[] response = client.get().uri("/api/v3/ticker/24hr")
            .retrieve().body(Ticker24h[].class);
        return response == null ? List.of() : List.of(response);
    }

    public static void validate(String symbol,String interval,int limit) {
        if(symbol==null || !symbol.matches("[A-Z0-9]{3,20}USDT") || !INTERVALS.contains(interval) || limit<70 || limit>10000)
            throw new IllegalArgumentException("Invalid symbol, interval or limit");
    }
    public static long intervalMillis(String interval) {
        return switch(interval) {
            case "5m" -> 300_000L; case "15m" -> 900_000L;
            case "1h" -> 3_600_000L; case "4h" -> 14_400_000L;
            case "1d" -> 86_400_000L; default -> throw new IllegalArgumentException("Invalid interval");
        };
    }
    public List<Candle> closedCandlesRange(String symbol,String interval,long start,long end,int limit) {
        validate(symbol,interval,Math.max(70,limit));
        if(limit<1 || limit>1000 || start>end) throw new IllegalArgumentException("Invalid candle range");
        Object[][] rows=client.get().uri(uri->uri.path("/api/v3/klines")
            .queryParam("symbol",symbol).queryParam("interval",interval)
            .queryParam("startTime",start).queryParam("endTime",end)
            .queryParam("limit",limit).build()).retrieve().body(Object[][].class);
        if(rows==null) return List.of();
        List<Candle> out=new ArrayList<>();
        long now=System.currentTimeMillis();
        for(Object[] row:rows) {
            if(row.length<8 || Long.parseLong(row[6].toString())>=now) continue;
            out.add(new Candle(Instant.ofEpochMilli(Long.parseLong(row[0].toString())),
                bd(row[1]),bd(row[2]),bd(row[3]),bd(row[4]),bd(row[5]),bd(row[7])));
        }
        return List.copyOf(out);
    }

    public List<Candle> closedCandles(String symbol, String interval, int limit) {
        if (symbol == null || !symbol.matches("[A-Z0-9]{3,20}USDT") || !INTERVALS.contains(interval)
            || limit < 70 || limit > 1000) throw new IllegalArgumentException("Invalid symbol, interval or limit");
        Object[][] rows = client.get().uri(uri -> uri.path("/api/v3/klines")
            .queryParam("symbol", symbol).queryParam("interval", interval)
            .queryParam("limit", limit).build()).retrieve().body(Object[][].class);
        if (rows == null) return List.of();
        List<Candle> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Object[] row : rows) {
            if (row.length < 8) continue;
            long closeTime = Long.parseLong(row[6].toString());
            if (closeTime >= now) continue; // never use the unfinished candle
            result.add(new Candle(Instant.ofEpochMilli(Long.parseLong(row[0].toString())),
                bd(row[1]), bd(row[2]), bd(row[3]), bd(row[4]), bd(row[5]), bd(row[7])));
        }
        return List.copyOf(result);
    }

    private static BigDecimal bd(Object v) { return new BigDecimal(v.toString()); }
    public record Ticker24h(String symbol, String lastPrice, String quoteVolume,
                            String priceChangePercent, String bidPrice, String askPrice) {}
}
