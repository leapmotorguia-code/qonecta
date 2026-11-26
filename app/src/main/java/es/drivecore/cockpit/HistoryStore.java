package es.drivecore.cockpit;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.room.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HistoryStore implements AutoCloseable {
    @Entity(tableName = "samples")
    public static class Sample {
        @PrimaryKey(autoGenerate = true) public long id;
        public long session, utcMillis;
        public Double speed, soc, fuel, outKw, inKw;
        public Sample(long session, long utcMillis, Double speed, Double soc, Double fuel, Double outKw, Double inKw) {
            this.session=session; this.utcMillis=utcMillis; this.speed=speed; this.soc=soc; this.fuel=fuel; this.outKw=outKw; this.inKw=inKw;
        }
    }
    @Dao public interface Samples {
        @Insert void add(Sample row);
        @Query("SELECT * FROM samples ORDER BY id DESC LIMIT 30") List<Sample> recent();
        @Query("DELETE FROM samples WHERE id NOT IN (SELECT id FROM samples ORDER BY id DESC LIMIT 30000)") void prune();
        @Query("DELETE FROM samples") void clear();
    }
    @Database(entities={Sample.class},version=1,exportSchema=false)
    public abstract static class Db extends RoomDatabase { public abstract Samples samples(); }
    public interface Result { void accept(String message); }
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Context context;
    private Db db;
    private volatile boolean closed;
    public HistoryStore(Context context) {this.context=context.getApplicationContext();}
    private Db database() { if(db==null)db=Room.databaseBuilder(context,Db.class,"drivecore_history.db").build();return db; }
    private void run(Runnable job,Result callback) {
        if(closed)return;
        executor.execute(()->{try{job.run();}catch(Exception e){reply(callback,"Historial no disponible: "+e.getClass().getSimpleName());}});
    }
    private void reply(Result callback,String value){ui.post(()->{if(!closed)callback.accept(value);});}
    public void save(Sample row,Result callback){run(()->{database().runInTransaction(()->{database().samples().add(row);database().samples().prune();});reply(callback,"Última muestra guardada");},callback);}
    public void list(Result callback){run(()->{StringBuilder b=new StringBuilder();for(Sample s:database().samples().recent()){b.append(new java.util.Date(s.utcMillis)).append("\nVel: ").append(s.speed==null?"—":s.speed).append(" km/h | SOC: ").append(s.soc==null?"—":s.soc).append("%\nSalida: ").append(s.outKw==null?"—":s.outKw).append(" kW | Entrada: ").append(s.inKw==null?"—":s.inKw).append(" kW\n\n");}reply(callback,b.length()==0?"Sin muestras guardadas":b.toString());},callback);}
    public interface Rows { void accept(List<Sample> rows); }
    public void recentRows(Rows rows, Result error){run(()->{List<Sample> result=database().samples().recent();ui.post(()->{if(!closed)rows.accept(result);});},error);}
    public void clear(Result callback){run(()->{database().samples().clear();reply(callback,"Historial borrado");},callback);}
    @Override public void close(){if(closed)return;closed=true;executor.execute(()->{if(db!=null)db.close();});executor.shutdown();}
}
