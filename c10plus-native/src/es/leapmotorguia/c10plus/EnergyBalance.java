package es.leapmotorguia.c10plus;

/** Pure calculation for measured battery energy, never a simulated car reading. */
public final class EnergyBalance {
    public enum Status { GAIN, LOSS, EQUAL, UNAVAILABLE }
    public static final class Result {
        public final Status status;
        public final double spentKwh, incomingKwh, netKwh, recoveryPct;
        public final boolean partial;
        private Result(Status s, double spent, double incoming, double net, double pct, boolean partial) {
            this.status=s;this.spentKwh=spent;this.incomingKwh=incoming;
            this.netKwh=net;this.recoveryPct=pct;this.partial=partial;
        }
        public boolean available() { return status!=Status.UNAVAILABLE; }
    }
    private EnergyBalance() {}
    /**
     * Positive power out of the battery -> spent; negative power -> incoming.
     * Incoming can include generator/external charging, not exclusively brake regeneration.
     * When coverage is incomplete, the result is only for measured intervals.
     */
    public static Result calculate(double spent, double incoming, boolean partial) {
        if(!Double.isFinite(spent)||!Double.isFinite(incoming)||spent<0||incoming<0) {
            return new Result(Status.UNAVAILABLE,Double.NaN,Double.NaN,Double.NaN,Double.NaN,partial);
        }
        double net=spent-incoming;
        if(!Double.isFinite(net)) return new Result(Status.UNAVAILABLE,Double.NaN,Double.NaN,Double.NaN,Double.NaN,partial);
        double ratio=spent>0?incoming*100d/spent:(incoming>0?Double.POSITIVE_INFINITY:0);
        // Tolerance is 1 Wh to avoid declaring loss because of floating point noise.
        Status s=net>0.001?Status.LOSS:net<-.001?Status.GAIN:Status.EQUAL;
        return new Result(s,spent,incoming,net,ratio,partial);
    }
}
