package es.leapmotorguia.c10plus;
import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/** Explicit human-observed compatibility tests, stored locally and separate
 * from automatic telemetry. No VIN, position, logins or network requests. */
public final class CompatibilityTestLog {
    public static final String[] TESTS = {
        "Android Auto · imagen y conexión",
        "Android Auto · audio y pantalla táctil",
        "Apple CarPlay con iPhone / adaptador",
        "Pantalla dividida y barra de UNIT",
        "Radio original y Spotify",
        "TV original y servicios web (estacionado)",
        "Consumos y gráficas con datos reales"
    };
    public static final String[] STATES = {"Sin probar","Funciona","Falla","No compatible / no disponible"};
    private static final String PREFS="c10plus_manual_tests_v1";
    private CompatibilityTestLog(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static int[] states(Context c){
        int[] result=new int[TESTS.length];
        SharedPreferences p=prefs(c);
        for(int i=0;i<result.length;i++){
            int s=p.getInt("test_"+i,0);
            result[i]=s>=0&&s<STATES.length?s:0;
        }
        return result;
    }
    public static String notes(Context c){return prefs(c).getString("notes","");}
    public static long timestamp(Context c){return prefs(c).getLong("at",0);}
    public static void save(Context c,int[] states,String notes,String profile){
        if(states==null||states.length!=TESTS.length)throw new IllegalArgumentException("test count");
        SharedPreferences.Editor e=prefs(c).edit();
        for(int i=0;i<states.length;i++){
            if(states[i]<0||states[i]>=STATES.length)throw new IllegalArgumentException("invalid state");
            e.putInt("test_"+i,states[i]);
        }
        e.putString("notes",notes==null?"":notes.substring(0,Math.min(500,notes.length())));
        e.putString("profile",profile==null?"Sin identificar":profile);
        e.putLong("at",System.currentTimeMillis());
        e.apply();
    }
    public static String configuredProfile(Context context) {
        try{
            Class<?> c=Class.forName("es.c10.unitplus.integrated.VehicleProfiles");
            String[] labels=(String[])c.getMethod("labels").invoke(null);
            Object index=c.getMethod("index",Context.class).invoke(null,context);
            int i=((Number)index).intValue();
            return labels!=null&&i>=0&&i<labels.length?labels[i]:"Sin identificar";
        }catch(Throwable e){return "Perfil no accesible";}
    }
    public static String report(Context ctx){
        long timestamp=timestamp(ctx);
        if(timestamp<=0)return "Pruebas manuales: aún no registradas.";
        SharedPreferences p=prefs(ctx);
        StringBuilder b=new StringBuilder();
        b.append("PRUEBAS REALIZADAS MANUALMENTE (no detección automática)\n");
        b.append("Fecha: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault())
            .format(new Date(timestamp))).append("\n");
        b.append("Perfil configurado entonces: ").append(p.getString("profile","Sin identificar")).append("\n");
        int[] status=states(ctx);
        for(int i=0;i<TESTS.length;i++)b.append("• ").append(TESTS[i]).append(": ").append(STATES[status[i]]).append("\n");
        String note=notes(ctx);
        if(!note.trim().isEmpty())b.append("Observaciones: ").append(note.trim()).append("\n");
        return b.toString();
    }
}
