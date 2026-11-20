package dev.cvalente.trading.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PositionSizerTest {
    @Test void calculatesQuantityFromOnePercentRiskBudget() {
        var quantity = PositionSizer.quantity(new BigDecimal("10000"), new BigDecimal("1"),
            new BigDecimal("100"), new BigDecimal("98"));
        assertEquals(new BigDecimal("50.00000000"), quantity);
    }
    @Test void rejectsInvalidLongSpotInputs() {
        assertThrows(IllegalArgumentException.class, () -> PositionSizer.quantity(
            new BigDecimal("10000"), new BigDecimal("1"), new BigDecimal("98"), new BigDecimal("100")));
    }

    @Test void riskAwareSizingIncludesCostsAndCapitalLimit() {
        var quantity = PositionSizer.quantity(new BigDecimal("10000"), new BigDecimal("1"),
            new BigDecimal("100"), new BigDecimal("98"), new BigDecimal("0.001"), new BigDecimal("0.001"));
        assertTrue(quantity.compareTo(new BigDecimal("50")) < 0);
        assertTrue(quantity.multiply(new BigDecimal("100.1")).compareTo(new BigDecimal("10000")) <= 0);
    }

    @Test void costAwareSizerRejectsInvalidMarketInputs() {
        assertThrows(IllegalArgumentException.class, () -> PositionSizer.quantity(
            new BigDecimal("0"), new BigDecimal("1"), new BigDecimal("100"),
            new BigDecimal("98"), BigDecimal.ZERO, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> PositionSizer.quantity(
            new BigDecimal("10000"), new BigDecimal("1"), new BigDecimal("98"),
            new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test void onePercentRiskSizingUsesStopDistance() {
        var quantity = PositionSizer.quantity(new BigDecimal("10000"), new BigDecimal("1"),
            new BigDecimal("100"), new BigDecimal("98"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("50.00000000"), quantity);
        assertEquals(new BigDecimal("100.00000000"), quantity.multiply(new BigDecimal("2")));
    }
}
