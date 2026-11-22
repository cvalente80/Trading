package dev.cvalente.trading.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionModelTest {
    @Test void stopUsesStopPriceWithAdverseSlippage() {
        var exit = ExecutionModel.existingPosition(105, 98, 106, 100, 110, 0.01).orElseThrow();
        assertEquals("STOP", exit.reason());
        assertEquals(99, exit.price(), 1e-12);
    }

    @Test void gapBelowStopUsesOpeningPriceWithAdverseSlippage() {
        var exit = ExecutionModel.existingPosition(95, 90, 106, 100, 110, 0.01).orElseThrow();
        assertEquals("STOP_GAP", exit.reason());
        assertEquals(94.05, exit.price(), 1e-12);
    }

    @Test void targetUsesTargetPriceWithAdverseSlippage() {
        var exit = ExecutionModel.existingPosition(105, 103, 112, 100, 110, 0.01).orElseThrow();
        assertEquals("TARGET", exit.reason());
        assertEquals(108.9, exit.price(), 1e-12);
    }

    @Test void stopWinsWhenStopAndTargetAreBothTouched() {
        var exit = ExecutionModel.existingPosition(105, 98, 112, 100, 110, 0).orElseThrow();
        assertEquals("STOP", exit.reason());
        assertEquals(100, exit.price(), 1e-12);
    }

    @Test void entryBarStopUsesOpenForAdverseGap() {
        var exit = ExecutionModel.entryBar(95, 90, 100, 0.01).orElseThrow();
        assertEquals("ENTRY_BAR_STOP", exit.reason());
        assertEquals(94.05, exit.price(), 1e-12);
    }

    @Test void entryBarStopUsesStopWhenOpenIsAboveStop() {
        var exit = ExecutionModel.entryBar(105, 98, 100, 0.01).orElseThrow();
        assertEquals("ENTRY_BAR_STOP", exit.reason());
        assertEquals(99, exit.price(), 1e-12);
    }

    @Test void noExitIsReturnedWhenNeitherStopNorTargetIsTouched() {
        assertTrue(ExecutionModel.existingPosition(105, 103, 108, 100, 110, 0.01).isEmpty());
        assertTrue(ExecutionModel.entryBar(105, 102, 100, 0.01).isEmpty());
    }
}
