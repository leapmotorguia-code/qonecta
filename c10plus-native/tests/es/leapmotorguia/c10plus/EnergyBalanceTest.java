package es.leapmotorguia.c10plus;
public final class EnergyBalanceTest {
    static void close(double actual,double expected) {
        if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-9) throw new AssertionError(actual+" != "+expected);
    }
    static void check(boolean v) {if(!v)throw new AssertionError();}
    public static void main(String[] ignored) {
        EnergyBalance.Result r=EnergyBalance.calculate(10,3,false);
        check(r.status==EnergyBalance.Status.LOSS);close(r.netKwh,7);close(r.recoveryPct,30);check(!r.partial);
        r=EnergyBalance.calculate(2,5,true);
        check(r.status==EnergyBalance.Status.GAIN);close(r.netKwh,-3);check(r.partial);
        r=EnergyBalance.calculate(4,4,false);
        check(r.status==EnergyBalance.Status.EQUAL);close(r.netKwh,0);
        r=EnergyBalance.calculate(0,2,false);
        check(r.status==EnergyBalance.Status.GAIN);close(r.netKwh,-2);
        r=EnergyBalance.calculate(0,0,false);check(r.status==EnergyBalance.Status.EQUAL);
        check(EnergyBalance.calculate(Double.NaN,0,false).status==EnergyBalance.Status.UNAVAILABLE);
        check(EnergyBalance.calculate(3,-1,false).status==EnergyBalance.Status.UNAVAILABLE);
        check(EnergyBalance.calculate(Double.POSITIVE_INFINITY,1,false).status==EnergyBalance.Status.UNAVAILABLE);
        System.out.println("EnergyBalance 8 scenarios OK");
    }
}
