package dev.cvalente.trading.research;

import dev.cvalente.trading.domain.Candle;
import dev.cvalente.trading.domain.StrategyType;

import java.util.ArrayList;
import java.util.List;

/** Deterministic train/validation/out-of-sample partitioning for research runs. */
public final class WalkForwardStudy {
    private WalkForwardStudy() {}
    public record Window(List<Candle> train, List<Candle> validation, List<Candle> outOfSample) {}

    public static Window split(List<Candle> candles, double trainFraction, double validationFraction) {
        if (candles == null || candles.isEmpty() || !Double.isFinite(trainFraction) ||
            !Double.isFinite(validationFraction) || trainFraction <= 0 || validationFraction <= 0 ||
            trainFraction + validationFraction >= 1) throw new IllegalArgumentException("Invalid walk-forward split");
        int trainEnd = (int) Math.floor(candles.size() * trainFraction);
        int validationEnd = trainEnd + (int) Math.floor(candles.size() * validationFraction);
        if (trainEnd < 1 || validationEnd >= candles.size()) throw new IllegalArgumentException("Insufficient candles for split");
        return new Window(List.copyOf(candles.subList(0, trainEnd)),
            List.copyOf(candles.subList(trainEnd, validationEnd)),
            List.copyOf(candles.subList(validationEnd, candles.size())));
    }

    public static List<ReplayBacktester.Report> evaluate(Window window, StrategyType strategy,
                                                         double initial, double riskPercent,
                                                         double fee, double slippage) {
        return evaluate(window, strategy, initial, BacktestConfig.defaults(riskPercent, fee, slippage));
    }

    public static List<ReplayBacktester.Report> evaluate(Window window, StrategyType strategy,
                                                         double initial, BacktestConfig config) {
        List<ReplayBacktester.Report> reports = new ArrayList<>();
        for (List<Candle> period : List.of(window.train(), window.validation(), window.outOfSample())) {
            if (period.size() < 70) throw new IllegalArgumentException("Each walk-forward period needs 70 candles");
            reports.add(ReplayBacktester.run(period, strategy, initial, config));
        }
        return List.copyOf(reports);
    }
}
