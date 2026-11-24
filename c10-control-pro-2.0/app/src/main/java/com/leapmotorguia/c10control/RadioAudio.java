package com.leapmotorguia.c10control;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
/** Audio-only player. No surface, WebView, video output or vehicle control. */
final class RadioAudio {
 static final String[] NAMES={"RNE Radio Nacional","Radio 3","Radio Clásica","Radio 5","Onda Cero","Europa FM","RAC1"};
 static final String[] URLS={
 "https://rtvelivestream.rtve.es/rtvesec/rne/rne_r1_main.m3u8",
 "https://rtvelivestream.rtve.es/rtvesec/rne/rne_r3_main.m3u8",
 "https://rtvelivestream.rtve.es/rtvesec/rne/rne_r2_main.m3u8",
 "https://rtvelivestream.rtve.es/rtvesec/rne/rne_r5_madrid_main.m3u8",
 "https://radio-atres-live.ondacero.es/api/livestream-redirect/OCAAC.aac",
 "https://radio-atres-live.ondacero.es/api/livestream-redirect/EFMAAC.aac",
 "https://playerservices.streamtheworld.com/api/livestream-redirect/RAC_1.mp3"};
 private final AudioManager manager;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final AudioAttributes attr=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
 private AudioFocusRequest focus; private MediaPlayer player; private Runnable deadline; private String state="Radio detenida";
 RadioAudio(Context c){manager=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);}
 String status(){return state;}
 void play(int n){if(n>=0&&n<NAMES.length)play(NAMES[n],URLS[n]);}
 void play(String name,String url){stop();if(url==null||!url.startsWith("https://")){state="Dirección de audio no compatible";return;}
 try{
 focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attr).setOnAudioFocusChangeListener(change->{if(change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){stop();state="Audio interrumpido por otra aplicación";}},handler).build();
 if(manager==null||manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stop();state="Audio no disponible";return;}
 MediaPlayer p=new MediaPlayer();player=p;p.setAudioAttributes(attr);state="Conectando: "+name;
 p.setOnPreparedListener(mp->{if(player!=mp)return;cancelDeadline();try{mp.start();state="En directo: "+name;}catch(RuntimeException e){fail("No se pudo iniciar "+name);}});
 p.setOnErrorListener((mp,w,e)->{if(player==mp)fail("Error de conexión o formato: "+name);return true;});
 p.setOnCompletionListener(mp->{if(player==mp)fail("Emisión finalizada: "+name);});
 p.setDataSource(url);deadline=()->{if(player==p)fail("Tiempo de conexión agotado: "+name);};handler.postDelayed(deadline,20000);p.prepareAsync();
 }catch(Exception e){fail("No se pudo conectar: "+name);}
 }
 private void cancelDeadline(){if(deadline!=null){handler.removeCallbacks(deadline);deadline=null;}}
 private void fail(String msg){stop();state=msg;}
 void stop(){cancelDeadline();MediaPlayer p=player;player=null;if(p!=null){try{p.release();}catch(Exception ignored){}}AudioFocusRequest f=focus;focus=null;if(f!=null&&manager!=null){try{manager.abandonAudioFocusRequest(f);}catch(Exception ignored){}}state="Radio detenida";}
}
