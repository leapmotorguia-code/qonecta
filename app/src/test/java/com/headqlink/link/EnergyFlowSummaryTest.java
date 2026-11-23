package com.headqlink.link;

import org.junit.Test;
import static org.junit.Assert.*;

public class EnergyFlowSummaryTest {
    @Test public void calculatesConsumptionAndRegenerationSeparately() {
        EnergyFlowSummary s = EnergyFlowSummary.of(12, 3, 60, false);
        assertTrue(s.ready);
        assertEquals(12, s.consumedKwh, 1e-9);
        assertEquals(3, s.recoveredKwh, 1e-9);
        assertEquals(9, s.netKwh, 1e-9);
        assertEquals(25, s.recoveryPercent, 1e-9);
        assertEquals(20, s.consumedPer100, 1e-9);
        assertEquals(5, s.recoveredPer100, 1e-9);
        assertEquals(15, s.netPer100, 1e-9);
        assertTrue(s.provenance.contains("ESTIMACIÓN"));
    }

    @Test public void permitsMoreRegenerationThanConsumption() {
        EnergyFlowSummary s = EnergyFlowSummary.of(2, 3, 20, false);
        assertEquals(-1, s.netKwh, 1e-9);
        assertEquals(150, s.recoveryPercent, 1e-9);
        assertEquals(-5, s.netPer100, 1e-9);
    }

    @Test public void cloudFitDoesNotClaimRealGrossMeasurements() {
        EnergyFlowSummary s = EnergyFlowSummary.of(10, 2, 80, true);
        assertTrue(s.provenance.contains("ESTIMACIÓN"));
        assertTrue(s.provenance.contains("no medidos"));
    }

    @Test public void neverMakesUpValuesWithoutSamples() {
        EnergyFlowSummary empty = EnergyFlowSummary.of(0, 0, 0, false);
        assertFalse(empty.ready);
        assertTrue(Double.isNaN(empty.netKwh));
        assertTrue(Double.isNaN(empty.recoveryPercent));
        EnergyFlowSummary bad = EnergyFlowSummary.of(Double.NaN, 3, 100, false);
        assertFalse(bad.ready);
        assertTrue(Double.isNaN(bad.consumedKwh));
        EnergyFlowSummary negative = EnergyFlowSummary.of(-3, 0, 50, false);
        assertFalse(negative.ready);
    }

    @Test public void zeroDistanceNeverProducesInfinity() {
        EnergyFlowSummary s = EnergyFlowSummary.of(2, 1, 0, false);
        assertTrue(s.ready);
        assertTrue(Double.isNaN(s.netPer100));
    }
}
