package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;
import java.util.List;

public final class CostSweep {
    private CostSweep() {}
    public static List<ReplayBacktester.Report> run(List<Candle> candles, StrategyType strategy,
                                                    double initial, double riskPercent, double slippage,
                                                    List<Double> fees) {
        if (fees == null || fees.isEmpty()) throw new IllegalArgumentException("At least one fee required");
        return fees.stream().map(fee -> ReplayBacktester.run(candles, strategy, initial, riskPercent, fee, slippage)).toList();
    }
}
