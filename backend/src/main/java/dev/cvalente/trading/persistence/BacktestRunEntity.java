package dev.cvalente.trading.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="backtest_runs", indexes={@Index(name="idx_backtest_runs_created", columnList="createdAt")})
public class BacktestRunEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false, length=20) public String symbol;
    @Column(nullable=false, length=10) public String strategy;
    @Column(nullable=false, length=10) public String intervalCode;
    public int candleCount;
    public double feeRate;
    public double slippageRate;
    public double initialEquity;
    public double finalEquity;
    public double netReturnPercent;
    public double buyHoldPercent;
    public int completedTrades;
    public double winRatePercent;
    public double profitFactor;
    public double maxDrawdownPercent;
    public double totalFees;
    public double expectancy;
    public double averageGain;
    public double averageLoss;
    public double averageDurationSeconds;
    public int maxConsecutiveLosses;
    public double randomBenchmarkPercent;
    @Column(length=600) public String note;
    public BacktestRunEntity() {}
}
