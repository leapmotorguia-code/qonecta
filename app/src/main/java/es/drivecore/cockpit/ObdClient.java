package es.drivecore.cockpit;
import android.bluetooth.*;
import android.os.SystemClock;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;
final class ObdClient implements AutoCloseable {
 static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 static final class Reading {final double value;final long time;Reading(double v){value=v;time=SystemClock.elapsedRealtime();}}
 final Map<String,Reading> readings=new ConcurrentHashMap<>();
 private volatile BluetoothSocket socket; private volatile String status="Desconectado";private volatile long generation=0;private final ExecutorService worker=Executors.newSingleThreadExecutor();private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor();
 String status(){return status;} Double read(String pid){Reading r=readings.get(pid);return r!=null&&SystemClock.elapsedRealtime()-r.time<12000?r.value:null;}
 synchronized void disconnect(){generation++; BluetoothSocket s=socket;socket=null;readings.clear();status="Desconectado";closeSocket(s);}
 static void closeSocket(BluetoothSocket s){if(s!=null)try{s.close();}catch(IOException ignored){}}
 synchronized void connect(BluetoothDevice device){disconnect();final long token=generation;status="Conectando";worker.execute(()->run(device,token));}
 private void run(BluetoothDevice device,long token){BluetoothSocket s=null;try{if(token!=generation)return;s=device.createRfcommSocketToServiceRecord(SPP);synchronized(this){if(token!=generation){closeSocket(s);return;}socket=s;}
 final BluetoothSocket connecting=s;ScheduledFuture<?> deadline=watchdog.schedule(()->closeSocket(connecting),12,TimeUnit.SECONDS);try{s.connect();}finally{deadline.cancel(false);}if(token!=generation)return;
 InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();for(String cmd:new String[]{"ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0"}){if(token!=generation)return;exchange(in,out,cmd,cmd.equals("ATZ")?5000:3500);}
 if(token==generation)status="Adaptador conectado · consultando PIDs";
 while(token==generation){for(String cmd:ObdProtocol.QUERIES){if(token!=generation)return;Double v=ObdProtocol.decode(cmd,exchange(in,out,cmd,4000));synchronized(this){if(token!=generation)return;if(v==null)readings.remove(cmd);else readings.put(cmd,new Reading(v));}}Thread.sleep(350);}
 }catch(Exception e){synchronized(this){if(token==generation){status="Conexión interrumpida: "+e.getClass().getSimpleName();readings.clear();}}}finally{closeSocket(s);}}
 private String exchange(InputStream in,OutputStream out,String cmd,int timeout)throws Exception{out.write((cmd+"\r").getBytes(StandardCharsets.US_ASCII));out.flush();long end=SystemClock.elapsedRealtime()+timeout;StringBuilder b=new StringBuilder();while(SystemClock.elapsedRealtime()<end){if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(in.available()==0){Thread.sleep(20);continue;}int c=in.read();if(c<0)throw new EOFException();if(c=='>')return b.toString();if(b.length()>4096)throw new IOException("respuesta demasiado larga");b.append((char)c);}throw new IOException("tiempo agotado");}
 public void close(){disconnect();worker.shutdownNow();watchdog.shutdownNow();}
}
