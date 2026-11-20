package es.c10.complemento
import org.junit.Assert.*
import org.junit.Test
import es.c10.complemento.performance.PerformanceTracker
import es.c10.complemento.efficiency.TripEnergyBalanceTracker
import es.c10.complemento.sentry.VehicleSentryMonitor
class TrackersTest {
 @Test fun interpolatedRecovery(){val p=PerformanceTracker();p.onSpeedUpdateKmh(78.0,0);p.onSpeedUpdateKmh(82.0,1_000_000_000);p.onSpeedUpdateKmh(110.0,2_000_000_000);val r=p.onSpeedUpdateKmh(130.0,3_000_000_000);assertEquals(2.0,r.single().durationSeconds,0.0001)}
 @Test fun staleGapCancels(){val p=PerformanceTracker();p.onSpeedUpdateKmh(79.0,0);p.onSpeedUpdateKmh(81.0,1_000_000_000);assertTrue(p.onSpeedUpdateKmh(130.0,8_000_000_000).isEmpty())}
 @Test fun stoppedLaunchIsLabelledThreeToHundred(){val p=PerformanceTracker();val r=mutableListOf<String>();listOf(0.0,0.0,10.0,50.0,90.0,110.0).forEachIndexed{i,v->p.onSpeedUpdateKmh(v,i*1_000_000_000L).forEach{r.add(it.label)}};assertEquals(listOf("3–100 km/h"),r)}
 @Test fun invalidSpeedCancels(){val p=PerformanceTracker();p.onSpeedUpdateKmh(79.0,0);p.onSpeedUpdateKmh(81.0,1_000_000_000);p.onSpeedUpdateKmh(Double.NaN,2_000_000_000);assertTrue(p.onSpeedUpdateKmh(130.0,3_000_000_000).isEmpty())}
 @Test fun chargeAndGeneratorAreSeparate(){val e=TripEnergyBalanceTracker();for(i in 0..60)e.onTelemetryTick(-10.0,15.0,i*1_000_000_000L);assertEquals(1.0/6,e.snapshot().chargedKwh,1e-8);assertEquals(0.25,e.snapshot().generatorKwh,1e-8);assertEquals(0.0,e.snapshot().dischargedKwh,0.0)}
 @Test fun signedCrossingIntegratesBothSides(){val e=TripEnergyBalanceTracker();e.onTelemetryTick(10.0,0.0,0);e.onTelemetryTick(-10.0,0.0,2_000_000_000);assertEquals(5.0/3600,e.snapshot().chargedKwh,1e-8);assertEquals(5.0/3600,e.snapshot().dischargedKwh,1e-8)}
 @Test fun energyGapNotIntegrated(){val e=TripEnergyBalanceTracker();e.onTelemetryTick(10.0,0.0,0);e.onTelemetryTick(10.0,0.0,60_000_000_000);assertEquals(0.0,e.snapshot().dischargedKwh,0.0);e.resetTrip();assertEquals(0.0,e.snapshot().netBatteryKwh,0.0)}
 @Test fun vibrationCooldown(){val s=VehicleSentryMonitor();s.arm();assertNull(s.evaluate(0,0f,0f,1f));assertNotNull(s.evaluate(1000,1f,0f,1f));assertNull(s.evaluate(2000,0f,0f,1f));assertNotNull(s.evaluate(7000,1f,0f,1f));s.disarm();assertNull(s.evaluate(9000,0f,0f,1f))}
}
