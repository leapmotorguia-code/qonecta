package es.leapmotorguia.c10plus;
public final class VehicleProfileHintsTest {
    private static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] z){
        VehicleProfileHints.Hint c=VehicleProfileHints.interpret("C10 REEV 215 CV");
        eq(c.model,"C10");eq(c.drivetrain,VehicleProfileHints.Drivetrain.REEV);
        eq(VehicleProfileHints.interpret("C10 BEV").drivetrain,VehicleProfileHints.Drivetrain.BEV);
        eq(VehicleProfileHints.interpret("B03X eléctrico").model,"B03X");
        eq(VehicleProfileHints.interpret("B03").model,"B03");
        eq(VehicleProfileHints.interpret("T03").model,"T03");
        eq(VehicleProfileHints.interpret("B10 REEV").drivetrain,VehicleProfileHints.Drivetrain.REEV);
        eq(VehicleProfileHints.interpret("C10").drivetrain,VehicleProfileHints.Drivetrain.UNKNOWN);
        eq(VehicleProfileHints.interpret("Vehículo sin identificar").model,"No identificado");
        eq(VehicleProfileHints.interpret(null).drivetrain,VehicleProfileHints.Drivetrain.UNKNOWN);
        System.out.println("VehicleProfileHints: 9 tests OK");
    }
}
