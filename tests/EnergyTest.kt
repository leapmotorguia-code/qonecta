import es.drivecore.cockpit.EnergyAccumulator
import kotlin.math.abs
fun main(){
 val e=EnergyAccumulator(); fun eq(a:Double,b:Double){check(abs(a-b)<1e-9){"$a != $b"}}
 e.sample(-36.0,1_000_000_000); e.sample(-36.0,2_000_000_000); eq(e.totals().outKwh,.01)
 e.sample(-36.0,2_000_000_000); eq(e.totals().outKwh,.01)
 e.sample(36.0,3_000_000_000); eq(e.totals().outKwh,.0125);eq(e.totals().inKwh,.0025)
 e.sample(36.0,4_000_000_000);eq(e.totals().inKwh,.0125);eq(e.totals().netKwh,0.0)
 e.sample(36.0,12_000_000_000);eq(e.totals().inKwh,.0125)
 e.sample(null,null);e.sample(36.0,13_000_000_000);eq(e.totals().inKwh,.0125)
 e.sample(Double.NaN,14_000_000_000);e.sample(36.0,15_000_000_000);eq(e.totals().inKwh,.0125)
 e.reset();check(e.totals().intervals==0);eq(e.totals().outKwh,0.0);eq(e.totals().inKwh,0.0)
 println("12 energy assertions passed")
}
