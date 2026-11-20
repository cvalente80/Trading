package dev.cvalente.trading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PositionSizer {
    private PositionSizer() {}

    public static BigDecimal quantity(BigDecimal equity, BigDecimal riskPercent,
                                      BigDecimal entry, BigDecimal stop) {
        validateInputs(equity, riskPercent, entry, stop);
        BigDecimal riskBudget = equity.multiply(riskPercent).divide(new BigDecimal("100"), 8, RoundingMode.DOWN);
        BigDecimal riskPerUnit = entry.subtract(stop);
        return riskBudget.divide(riskPerUnit, 8, RoundingMode.DOWN);
    }

    public static BigDecimal quantity(BigDecimal equity, BigDecimal riskPercent,
                                      BigDecimal entry, BigDecimal stop,
                                      BigDecimal feeRate, BigDecimal slippageRate) {
        validateInputs(equity, riskPercent, entry, stop);
        if (feeRate == null || slippageRate == null || feeRate.signum() < 0 ||
                slippageRate.signum() < 0 || feeRate.compareTo(BigDecimal.ONE) >= 0 ||
                slippageRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Invalid trading cost inputs");
        }
        BigDecimal riskBudget = equity.multiply(riskPercent).divide(new BigDecimal("100"), 12, RoundingMode.DOWN);
        BigDecimal entryCost = entry.multiply(BigDecimal.ONE.add(slippageRate));
        BigDecimal stopProceeds = stop.multiply(BigDecimal.ONE.subtract(slippageRate));
        BigDecimal perUnit = entryCost.subtract(stopProceeds)
            .add(entryCost.multiply(feeRate)).add(stopProceeds.multiply(feeRate));
        BigDecimal affordable = equity.divide(entryCost.multiply(BigDecimal.ONE.add(feeRate)), 12, RoundingMode.DOWN);
        return riskBudget.divide(perUnit, 8, RoundingMode.DOWN).min(affordable);
    }

    private static void validateInputs(BigDecimal equity, BigDecimal riskPercent,
                                       BigDecimal entry, BigDecimal stop) {
        if (equity == null || riskPercent == null || entry == null || stop == null
                || equity.signum() <= 0 || riskPercent.signum() <= 0
                || entry.signum() <= 0 || stop.signum() <= 0 || entry.compareTo(stop) <= 0) {
            throw new IllegalArgumentException("Invalid long spot position sizing inputs");
        }
    }
}
