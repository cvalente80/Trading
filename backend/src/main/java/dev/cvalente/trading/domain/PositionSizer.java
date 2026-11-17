package dev.cvalente.trading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PositionSizer {
    private PositionSizer() {}

    public static BigDecimal quantity(BigDecimal equity, BigDecimal riskPercent,
                                      BigDecimal entry, BigDecimal stop) {
        if (equity == null || riskPercent == null || entry == null || stop == null
                || entry.signum() <= 0 || stop.signum() <= 0 || entry.compareTo(stop) <= 0
                || riskPercent.signum() <= 0) {
            throw new IllegalArgumentException("Invalid long spot position sizing inputs");
        }
        BigDecimal riskBudget = equity.multiply(riskPercent).divide(new BigDecimal("100"), 8, RoundingMode.DOWN);
        BigDecimal riskPerUnit = entry.subtract(stop);
        return riskBudget.divide(riskPerUnit, 8, RoundingMode.DOWN);
    }
}
