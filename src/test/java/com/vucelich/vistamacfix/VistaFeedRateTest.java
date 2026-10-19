package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VistaFeedRateTest {
    @Test
    void unsetCapLeavesVistaRateAlone() {
        assertEquals(0.5, VistaFeedRate.capPerTick(0.5, 0.0));
        assertEquals(0.5, VistaFeedRate.capPerTick(0.5, -1.0));
        assertEquals(Double.NaN, VistaFeedRate.capPerTick(Double.NaN, 5.0));
    }

    @Test
    void capUsesTheSameTickUnitAsVistaBaseFps() {
        assertEquals(5.0 / 20.0, VistaFeedRate.capPerTick(10.0 / 20.0, 5.0));
        assertEquals(3.0 / 20.0, VistaFeedRate.capPerTick(0.5, 3.0));
        assertEquals(0.2, VistaFeedRate.capPerTick(0.2, 10.0));
    }
}
