package dev.cvalente.trading.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="market_candles", uniqueConstraints=@UniqueConstraint(name="uq_candle_symbol_interval_time",columnNames={"symbol","candle_interval","open_time"}), indexes=@Index(name="ix_candle_lookup",columnList="symbol,candle_interval,open_time"))
public class CandleEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=24) public String symbol;
    @Column(name="candle_interval",nullable=false,length=8) public String interval;
    @Column(name="open_time",nullable=false) public Instant openTime;
    @Column(nullable=false,precision=30,scale=12) public BigDecimal open;
    @Column(nullable=false,precision=30,scale=12) public BigDecimal high;
    @Column(nullable=false,precision=30,scale=12) public BigDecimal low;
    @Column(nullable=false,precision=30,scale=12) public BigDecimal close;
    @Column(nullable=false,precision=38,scale=12) public BigDecimal volume;
    @Column(name="quote_volume",nullable=false,precision=38,scale=12) public BigDecimal quoteVolume;
}
