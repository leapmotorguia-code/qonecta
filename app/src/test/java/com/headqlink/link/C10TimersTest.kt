package com.headqlink.link

import org.junit.Assert.*
import org.junit.Test

class C10TimersTest {
    @Test fun requiresThreeSecondsStoppedAndRecordsEveryTarget() {
        val t = LaunchTimer(true)
        t.fix(0.0, 0.0); t.fix(1000.0, 0.0); t.fix(2000.0, 0.0)
        t.fix(3000.0, 0.0); t.fix(4000.0, 10.0); t.fix(5000.0, 30.0)
        assertTrue(t.fix(6000.0, 55.0)!!.contains("0-50"))
        assertTrue(t.fix(7000.0, 105.0)!!.contains("0-100"))
        assertTrue(t.fix(8000.0, 125.0)!!.contains("0-120"))
        assertTrue(t.fix(9000.0, 135.0)!!.contains("0-130"))
        val all = t.fix(10000.0, 145.0)!!
        for (target in listOf("0-50", "0-100", "0-120", "0-130", "0-140")) assertTrue(all.contains(target))
    }
    @Test fun noLaunchAfterShortStopOrMissingGps() {
        val t = LaunchTimer(true)
        t.fix(0.0, 0.0); t.fix(1000.0, 0.0); t.fix(2000.0, 0.0)
        t.fix(3000.0, 10.0); t.fix(4000.0, 30.0)
        assertNull(t.fix(5000.0, 100.0))
        t.reset(); for (i in 0..3) t.fix(i * 1000.0, 0.0)
        t.fix(8000.0, 10.0); t.fix(9000.0, 50.0)
        assertNull(t.fix(10000.0, 100.0))
    }
    @Test fun rollingRequiresStable80AndCancelsGpsGap() {
        val t = RollingTimer()
        for (i in 0..3) assertNull(t.fix(i * 1000.0, 80.0))
        assertNull(t.fix(4000.0, 85.0)); assertNull(t.fix(5000.0, 105.0))
        assertTrue(t.fix(6000.0, 125.0)!!.contains("80-120"))
        assertTrue(t.fix(7000.0, 135.0)!!.contains("80-130"))
        assertTrue(t.fix(8000.0, 145.0)!!.contains("80-140"))
        t.reset(); for (i in 0..3) t.fix(i * 1000.0, 80.0)
        t.fix(7000.0, 85.0); assertNull(t.fix(8000.0, 125.0))
    }
    @Test fun calculatorRejectsInvalidDataAndCombinesCosts() {
        assertEquals(6.8, ReevCost.total(18.0, 2.0, 0.2, 1.6, 100.0), 0.00001)
        assertEquals(13.6, ReevCost.total(18.0, 2.0, 0.2, 1.6, 200.0), 0.00001)
        assertTrue(ReevCost.total(-1.0, 0.0, 0.2, 1.6, 100.0).isNaN())
        assertTrue(ReevCost.total(Double.NaN, 0.0, 0.2, 1.6, 100.0).isNaN())
    }
}
