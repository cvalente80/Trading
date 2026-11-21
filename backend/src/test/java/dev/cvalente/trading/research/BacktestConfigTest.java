package dev.cvalente.trading.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BacktestConfigTest {
    @Test void conservativeSizingIsHalfPercent() {
        assertEquals(.5, BacktestConfig.conservative(.001, .0005).riskPercent());
    }

    @Test void invalidEmaDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            new BacktestConfig(.5, .001, .0005, false, false, false, false, false, false, 0, 0, -1, 20, 45, 68));
    }

    @Test void trailingAndTimeExitAreConfigurable() {
        var config = new BacktestConfig(.5, .001, .0005, false, false, false, false,
            false, false, 2, 12, 3, 20, 45, 68);
        assertEquals(2, config.trailingStopAtr());
        assertEquals(12, config.maxHoldingCandles());
    }
}
