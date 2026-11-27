package es.drivecore.connect;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.widget.*;
import android.view.View;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;

public class MainActivity extends Activity {
    private LocalReceiver receiver;private TextView status,address,code,info;private Button export;private File staged,folder;
    private boolean valid;
    private static final int SAVE=41;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);folder=new File(getFilesDir(),"inbox");folder.mkdirs();staged=new File(folder,"received.apk");
        new File(folder,"upload.part").delete();
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);root.setBackgroundColor(0xff061c29);
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        title(root,"DRIVECORE CONNECT",26);title(root,"Receptor local · 0.1",18);
        title(root,"iPhone / Android · Safari o Chrome",15);
        title(root,"Prueba con el vehículo estacionado. Solo red privada de confianza. HTTP local sin cifrado: no enviar datos personales ni contraseñas. No instala aplicaciones ni cambia permisos.",15);
        status=title(root,"Sesión cerrada",16);address=title(root,"",18);code=title(root,"",16);info=title(root,"",15);
        btn(root,"Iniciar sesión de 5 minutos",v->chooseNetwork());
        btn(root,"Cerrar conexión",v->stop());
        export=btn(root,"Guardar APK para revisión",v->save());export.setEnabled(false);
        btn(root,"Eliminar archivo recibido",v->new AlertDialog.Builder(this).setMessage("¿Eliminar el archivo pendiente?").setPositiveButton("Eliminar",(d,w)->{stop();staged.delete();valid=false;info.setText("");export.setEnabled(false);}).setNegativeButton("Cancelar",null).show());
        if(staged.exists())inspect();
    }
    TextView title(LinearLayout p,String s,int size){TextView t=new TextView(this);t.setTextColor(0xffd9faff);t.setTextSize(size);t.setTextIsSelectable(true);t.setPadding(0,10,0,10);t.setText(s);p.addView(t);return t;}
    Button btn(LinearLayout p,String text,View.OnClickListener l){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(l);p.addView(b);return b;}
    private void chooseNetwork(){
        if(staged.exists()){status.setText("Revisa o elimina primero el archivo recibido");return;}
        stop();ArrayList<InetAddress> list=new ArrayList<>();
        try{Enumeration<NetworkInterface> ns=NetworkInterface.getNetworkInterfaces();while(ns.hasMoreElements()){NetworkInterface ni=ns.nextElement();if(!ni.isUp()||ni.isLoopback()||ni.isVirtual())continue;Enumeration<InetAddress> as=ni.getInetAddresses();while(as.hasMoreElements()){InetAddress a=as.nextElement();if(a instanceof Inet4Address&&a.isSiteLocalAddress())list.add(a);}}}catch(Exception e){status.setText("Error de red");return;}
        if(list.isEmpty()){status.setText("Conecta el equipo a una red Wi-Fi privada");return;}
        String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=list.get(i).getHostAddress();
        new AlertDialog.Builder(this).setTitle("Selecciona IP local de la red privada").setItems(names,(d,i)->start(list.get(i))).setNegativeButton("Cancelar",null).show();
    }
    private void start(InetAddress a){
        try(InputStream in=getAssets().open("connect.html")){
            ByteArrayOutputStream page=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)page.write(b,0,n);
            final LocalReceiver[] ref=new LocalReceiver[1];
            LocalReceiver r=new LocalReceiver(a,8787,folder,page.toByteArray(),new LocalReceiver.Listener(){
                public void message(String s){runOnUiThread(()->{if(receiver==ref[0]){status.setText(s);if(s.equals("Sesión cerrada")){address.setText("");code.setText("");receiver=null;}}});}
                public void received(File f,long n,String hash){runOnUiThread(()->{if(!isDestroyed())inspect();});}
            });
            ref[0]=r;receiver=r;address.setText(r.url());code.setText("Código temporal: "+r.code());status.setText("Abre la dirección en Safari o Chrome. Copia el código.");r.start();
        }catch(Exception e){status.setText("No se pudo abrir el receptor: "+e.getClass().getSimpleName());}
    }
    private void inspect(){
        export.setEnabled(false);valid=false;info.setText("Comprobando archivo…");
        new Thread(()->{try{
            PackageInfo p=getPackageManager().getPackageArchiveInfo(staged.getAbsolutePath(),0);
            if(p==null)throw new IOException("No es APK válida");
            MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(staged)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
            StringBuilder hash=new StringBuilder();for(byte x:md.digest())hash.append(String.format(Locale.ROOT,"%02x",x&255));
            String result="APK recibida: "+p.packageName+"\nVersión: "+p.versionName+"\nSHA-256: "+hash+"\nComparar con el hash publicado por el autor. No se ha verificado la confianza de su firma.";
            runOnUiThread(()->{if(!isDestroyed()&&staged.exists()){info.setText(result);valid=true;export.setEnabled(true);}});
        }catch(Exception e){runOnUiThread(()->{if(!isDestroyed())info.setText("Archivo rechazado: no se pudo leer como APK");});}},"inspect").start();
    }
    private void save(){if(!valid)return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/vnd.android.package-archive").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"DriveCore-recibida.apk");try{startActivityForResult(i,SAVE);}catch(Exception e){status.setText("El sistema no ofrece selector de archivos");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=SAVE||result!=RESULT_OK||data==null||data.getData()==null||!valid)return;
        new Thread(()->{try(InputStream in=new FileInputStream(staged);OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);runOnUiThread(()->status.setText("Guardada. Instalación independiente, solo por el sistema autorizado."));}catch(Exception e){runOnUiThread(()->status.setText("No se pudo guardar el archivo"));}}).start();
    }
    private void stop(){LocalReceiver r=receiver;receiver=null;if(r!=null)r.close();if(status!=null){status.setText("Sesión cerrada");address.setText("");code.setText("");}}
    @Override protected void onPause(){stop();super.onPause();}
    @Override protected void onDestroy(){stop();super.onDestroy();}
}
