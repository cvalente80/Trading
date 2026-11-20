package dev.cvalente.trading.market;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.persistence.CandleEntity;
import dev.cvalente.trading.persistence.CandleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Historical candles cached in PostgreSQL, with idempotent INSERT ON CONFLICT. */
@Service
public class CandleHistoryService {
    private final BinancePublicMarketClient market;
    private final CandleRepository repo;
    private final JdbcTemplate jdbc;
    public CandleHistoryService(BinancePublicMarketClient market,CandleRepository repo,JdbcTemplate jdbc) {
        this.market=market; this.repo=repo; this.jdbc=jdbc;
    }
    public record History(List<Candle> candles,int fetched,long stored,boolean complete) {}

    public History load(String symbol,String interval,int limit) {
        BinancePublicMarketClient.validate(symbol,interval,limit);
        long step=BinancePublicMarketClient.intervalMillis(interval);
        long end=System.currentTimeMillis();
        // Last fully closed candle must have closeTime strictly before now.
        long lastOpen=(end/step)*step-step;
        long firstOpen=lastOpen-(long)(limit-1)*step;
        Instant start=Instant.ofEpochMilli(firstOpen);
        Instant exclusiveEnd=Instant.ofEpochMilli(lastOpen+step);
        List<CandleEntity> existing=repo.findBySymbolAndIntervalAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(symbol,interval,start,exclusiveEnd);
        Set<Long> present=new HashSet<>();
        for(CandleEntity c:existing) present.add(c.openTime.toEpochMilli());
        int fetched=0;
        // Fetch only missing contiguous ranges; Binance API supports max 1000 candles per request.
        long cursor=firstOpen;
        while(cursor<=lastOpen) {
            if(present.contains(cursor)) {cursor+=step;continue;}
            long gapStart=cursor;
            int count=0;
            while(cursor<=lastOpen && !present.contains(cursor) && count<1000) {cursor+=step;count++;}
            List<Candle> downloaded=market.closedCandlesRange(symbol,interval,gapStart,cursor-1,count);
            for(Candle c:downloaded) {
                long t=c.openTime().toEpochMilli();
                if(t<gapStart || t>=cursor || (t-gapStart)%step!=0 || present.contains(t)) continue;
                jdbc.update("INSERT INTO market_candles(symbol,candle_interval,open_time,open,high,low,close,volume,quote_volume) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(symbol,candle_interval,open_time) DO NOTHING",
                    symbol,interval,java.sql.Timestamp.from(c.openTime()),c.open(),c.high(),c.low(),c.close(),c.volume(),c.quoteVolume());
                present.add(t); fetched++;
            }
        }
        List<Candle> candles=repo.findBySymbolAndIntervalAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(symbol,interval,start,exclusiveEnd)
            .stream().map(c->new Candle(c.openTime,c.open,c.high,c.low,c.close,c.volume,c.quoteVolume)).toList();
        boolean complete = CandleSeriesValidator.isComplete(candles, start, step, limit);
        return new History(candles,fetched,repo.countBySymbolAndInterval(symbol,interval),complete);
    }
}
