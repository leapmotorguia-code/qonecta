package com.headqlink.link;

import com.leapmotorguia.c10nativa.ObdValues;
import com.leapmotorguia.c10nativa.PerformanceTimer;
import org.junit.Test;
import static org.junit.Assert.*;

public final class C10RebuildTest {
    @Test public void decodesOnlyStandardPositivePidFrames(){
        assertEquals(1726.0,ObdValues.decode("0C","010C\r41 0C 1A F8\r>"),0.001);
        assertEquals(88.0,ObdValues.decode("0D","7E8 03 41 0D 58 >"),0.001);
        assertEquals(80.0,ObdValues.decode("05","41 05 78>"),0.001);
        assertEquals(100.0,ObdValues.decode("04","41 04 FF>"),0.001);
        assertNull(ObdValues.decode("0C","NO DATA>"));
        assertNull(ObdValues.decode("0C","7F 01 12>"));
        assertNull(ObdValues.decode("0C","410C1A>"));
        assertNull(ObdValues.decode("0D","410C1AF8>"));
        assertNull(ObdValues.decode("99","419900>"));
    }
    @Test public void adapterVoltageIsNotTractionVoltage(){
        assertEquals(12.6,ObdValues.adapterVolts("ATRV\r12.6V\r>"),0.0001);
        assertNull(ObdValues.adapterVolts("NO DATA"));assertNull(ObdValues.adapterVolts("400V"));
    }
    @Test public void timingInterpolatesAndInvalidatesGpsGaps(){
        PerformanceTimer t=new PerformanceTimer(PerformanceTimer.Mode.FROM_STOP);
        for(int i=0;i<=3;i++)assertTrue(t.accept(i*1000,0));
        assertEquals(PerformanceTimer.State.ARMED,t.getState());
        assertTrue(t.accept(4000,10));assertTrue(t.accept(5000,50));assertTrue(t.accept(6000,100));
        assertEquals(Long.valueOf(2700),t.elapsedAt(100));
        assertFalse(t.accept(10000,110));assertNull(t.elapsedAt(100));
    }
    @Test public void recoveryRequiresStable80AndFreshFixes(){
        PerformanceTimer t=new PerformanceTimer(PerformanceTimer.Mode.RECOVERY);
        for(int i=0;i<=3;i++)t.accept(i*1000,80);
        t.accept(4000,90);t.accept(5000,120);
        assertEquals(Long.valueOf(2000),t.elapsedAt(120));
        assertFalse(t.accept(5000,Double.NaN));assertNull(t.elapsedAt(120));
    }
}
