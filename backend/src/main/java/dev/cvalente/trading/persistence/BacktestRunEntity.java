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
    @Column(nullable=false, length=64, columnDefinition="varchar(64) default 'legacy'") public String strategyVersion = "legacy";
    @Column(nullable=false, columnDefinition="text default ''") public String configuration = "";
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
    @Column(nullable=false, columnDefinition="double precision default 0") public double expectancy;
    @Column(nullable=false, columnDefinition="double precision default 0") public double averageGain;
    @Column(nullable=false, columnDefinition="double precision default 0") public double averageLoss;
    @Column(nullable=false, columnDefinition="double precision default 0") public double averageDurationSeconds;
    @Column(nullable=false, columnDefinition="integer default 0") public int maxConsecutiveLosses;
    @Column(nullable=false, columnDefinition="double precision default 0") public double randomBenchmarkPercent;
    @Column(length=600) public String note;
    public BacktestRunEntity() {}
}
