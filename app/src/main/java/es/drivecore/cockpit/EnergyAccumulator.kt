package es.drivecore.cockpit

data class EnergyTotals(val outKwh: Double=0.0, val inKwh: Double=0.0, val intervals: Int=0) { val netKwh get() = outKwh-inKwh }
/** Integrates actual AAOS battery power samples, never interpolating long gaps or counting duplicates. */
class EnergyAccumulator {
    private var previous: Pair<Long,Double>? = null
    private var out=0.0; private var input=0.0; private var intervals=0
    fun sample(kw: Double?, ns: Long?) {
        if (kw == null || ns == null || ns <= 0 || !kw.isFinite() || kotlin.math.abs(kw)>1000) { previous=null; return }
        val p=previous
        if (p != null && ns == p.first) return
        previous=ns to kw
        if (p==null) return
        val dt=ns-p.first
        if(dt !in 1L..3_000_000_000L) return
        val hours=dt/3.6e12
        // Piecewise-linear estimate, split intervals crossing zero.
        val a=p.second; val b=kw
        if(a*b<0) {
            val f=kotlin.math.abs(a)/(kotlin.math.abs(a)+kotlin.math.abs(b))
            add(a/2*hours*f); add(b/2*hours*(1-f))
        } else add((a+b)/2*hours)
        intervals++
    }
    private fun add(kwh: Double) { if(kwh>=0) input+=kwh else out-=kwh }
    fun totals()=EnergyTotals(out,input,intervals)
    fun reset(){ previous=null; out=0.0; input=0.0; intervals=0 }
}
