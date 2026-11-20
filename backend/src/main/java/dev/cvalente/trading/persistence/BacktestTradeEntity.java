package dev.cvalente.trading.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="backtest_trades", indexes={@Index(name="idx_backtest_trades_run", columnList="run_id")})
public class BacktestTradeEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="run_id",nullable=false)
    public BacktestRunEntity run;
    public int entryIndex;
    public int exitIndex;
    public Instant entryTime;
    public Instant exitTime;
    public double entryPrice;
    public Double stopLoss;
    public Double takeProfit;
    public double exitPrice;
    public double quantity;
    public Double entryFee;
    public Double exitFee;
    public double netPnl;
    @Column(length=40) public String reason;
    public BacktestTradeEntity() {}
}
