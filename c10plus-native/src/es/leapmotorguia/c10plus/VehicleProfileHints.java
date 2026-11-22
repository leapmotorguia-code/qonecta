package es.leapmotorguia.c10plus;
import java.text.Normalizer;
import java.util.Locale;

/** Information-only parsing of a manually selected vehicle profile.
 * Never claims to identify a connected vehicle.
 */
public final class VehicleProfileHints {
    public enum Drivetrain { BEV, REEV, UNKNOWN }
    public static final class Hint {
        public final String model;
        public final Drivetrain drivetrain;
        Hint(String m,Drivetrain d){model=m;drivetrain=d;}
        public String friendlyDrivetrain(){
            return drivetrain==Drivetrain.BEV?"Eléctrico (BEV)":
                drivetrain==Drivetrain.REEV?"Autonomía extendida (REEV)":"Por confirmar";
        }
        public String guidance(){
            if(drivetrain==Drivetrain.BEV) return "Perfil eléctrico: el gasto y la entrada de batería pueden calcularse si hay telemetría. No se presume combustible ni generador.";
            if(drivetrain==Drivetrain.REEV) return "Perfil REEV: separar energía de batería, aportación del generador y gasolina requiere señales reales independientes.";
            return "Tipo de propulsión pendiente de confirmar. No se aplican consumos ni capacidades supuestos.";
        }
    }
    private VehicleProfileHints(){}
    private static String normalize(String s){
        String x=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toUpperCase(Locale.ROOT);
        return x.replaceAll("[^A-Z0-9]+"," ").trim();
    }
    public static Hint interpret(String label) {
        String norm=normalize(label);
        String model="No identificado";
        if(norm.contains("B03X")||norm.contains("B03 X"))model="B03X";
        else for(String s:new String[]{"C10","B10","B05","B03","T03"}){
            if(norm.contains(s)){model=s;break;}
        }
        boolean reev=norm.contains("REEV")||norm.contains("EREV")||
            norm.contains("RANGE EXTENDER")||norm.contains("AUTONOMIA EXTENDIDA");
        boolean bev=norm.contains("BEV")||norm.contains("ELECTRICO")||
            norm.contains("100 ELECTRIC")||norm.contains("BATTERY ELECTRIC");
        Drivetrain drive=reev&&!bev?Drivetrain.REEV:
            bev&&!reev?Drivetrain.BEV:Drivetrain.UNKNOWN;
        return new Hint(model,drive);
    }
}
