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

public class ConnectActivity extends Activity {
    private LocalReceiver receiver;private TextView status,address,code,info;private Button export,install;private String inspectedHash;private File staged,folder;
    private boolean valid;
    private static final int SAVE=41, INSTALL=42;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);folder=new File(getFilesDir(),"connect-inbox");folder.mkdirs();staged=new File(folder,"received.apk");
        new File(folder,"upload.part").delete();
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);root.setBackgroundColor(0xff061c29);
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        title(root,"DRIVECORE CONNECT",26);title(root,"Actualizaciones Premium · 1.0",18);
        title(root,"iPhone / Android · Safari o Chrome",15);btn(root,"Volver a instrumentación",v->finish());
        title(root,"Prueba con el vehículo estacionado. Solo red privada de confianza. HTTP local sin cifrado: no enviar datos personales ni contraseñas. Solo actualizaciones del mismo paquete y certificado. Confirmación local obligatoria.",15);
        status=title(root,"Sesión cerrada",16);address=title(root,"",18);code=title(root,"",16);info=title(root,"",15);
        btn(root,"Iniciar sesión de 5 minutos",v->chooseNetwork());
        btn(root,"Cerrar conexión",v->stop());
        export=btn(root,"Guardar APK para revisión",v->save());export.setEnabled(false);
        install=btn(root,"Solicitar instalación Android",v->prepareInstall());install.setEnabled(false);
        btn(root,"Eliminar archivo recibido",v->new AlertDialog.Builder(this).setMessage("¿Eliminar el archivo pendiente?").setPositiveButton("Eliminar",(d,w)->{stop();staged.delete();valid=false;info.setText("");export.setEnabled(false);install.setEnabled(false);inspectedHash=null;}).setNegativeButton("Cancelar",null).show());
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
        export.setEnabled(false);install.setEnabled(false);valid=false;inspectedHash=null;info.setText("Comprobando archivo…");
        new Thread(()->{try{
            PackageInfo p=getPackageManager().getPackageArchiveInfo(staged.getAbsolutePath(),0);
            if(p==null)throw new IOException("No es APK válida");
            MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(staged)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
            StringBuilder hash=new StringBuilder();for(byte x:md.digest())hash.append(String.format(Locale.ROOT,"%02x",x&255));
            String result="APK recibida: "+p.packageName+"\nVersión: "+p.versionName+"\nSHA-256: "+hash+"\nComparar con el hash publicado por el autor. No se ha verificado la confianza de su firma.";
            runOnUiThread(()->{if(!isDestroyed()&&staged.exists()){info.setText(result);valid=true;inspectedHash=hash.toString();export.setEnabled(true);install.setEnabled(true);}});
        }catch(Exception e){runOnUiThread(()->{if(!isDestroyed())info.setText("Archivo rechazado: no se pudo leer como APK");});}},"inspect").start();
    }
    private void prepareInstall(){
        if(!valid||inspectedHash==null||!staged.isFile())return;
        try {
            android.content.pm.PackageManager pm=getPackageManager();
            PackageInfo incoming=pm.getPackageArchiveInfo(staged.getAbsolutePath(),android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
            PackageInfo current=pm.getPackageInfo(getPackageName(),android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
            if(incoming==null||!getPackageName().equals(incoming.packageName)||incoming.getLongVersionCode()<=current.getLongVersionCode()||incoming.signingInfo==null||current.signingInfo==null||
                !new java.util.HashSet<>(java.util.Arrays.asList(incoming.signingInfo.getApkContentsSigners())).equals(new java.util.HashSet<>(java.util.Arrays.asList(current.signingInfo.getApkContentsSigners())))){
                status.setText("Actualización rechazada: paquete, versión o certificado diferente.");return;
            }
        }catch(Exception e){status.setText("No se pudo verificar la firma del paquete");return;}
        if(!getPackageManager().canRequestPackageInstalls()){
            status.setText("El sistema no ha autorizado instalación desde DriveCore Connect.");
            new AlertDialog.Builder(this).setMessage("Si tu dispositivo lo permite, revisa la autorización de instalación de aplicaciones para DriveCore Connect. No se cambiarán ajustes automáticamente.").setPositiveButton("Abrir ajustes",(d,w)->{
                try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,android.net.Uri.parse("package:"+getPackageName())));}catch(Exception e){status.setText("Ajuste no disponible en este sistema");}
            }).setNegativeButton("Cancelar",null).show();return;
        }
        final EditText expected=new EditText(this);expected.setSingleLine(true);expected.setHint("SHA-256 de la fuente de confianza");
        new AlertDialog.Builder(this).setTitle("Comprobar archivo antes de instalar").setMessage("Pega el SHA-256 publicado por el autor en una fuente independiente y fiable. No copies el hash recibido como sustituto de esa comprobación. Confirma únicamente con el coche estacionado.").setView(expected)
            .setPositiveButton("Comprobar",(d,w)->{
                String h=expected.getText().toString().trim();
                if(!h.matches("(?i)[0-9a-f]{64}")||!h.equalsIgnoreCase(inspectedHash)){status.setText("Hash no coincide. No se solicita instalación.");return;}
                stop();
                try{
                    android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".connectfiles",staged);
                    Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT,true);
                    status.setText("Solicitud enviada al instalador del sistema. Confirma solo si es la aplicación esperada.");
                    startActivityForResult(i,INSTALL);
                }catch(Exception e){status.setText("Instalador no disponible: "+e.getClass().getSimpleName());}
            }).setNegativeButton("Cancelar",null).show();
    }
    private void save(){if(!valid)return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/vnd.android.package-archive").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"DriveCore-recibida.apk");try{startActivityForResult(i,SAVE);}catch(Exception e){status.setText("El sistema no ofrece selector de archivos");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==INSTALL){status.setText(result==RESULT_OK?"Instalador notificó éxito": "Instalación no confirmada por el sistema (resultado "+result+")");return;}if(request!=SAVE||result!=RESULT_OK||data==null||data.getData()==null||!valid)return;
        new Thread(()->{try(InputStream in=new FileInputStream(staged);OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);runOnUiThread(()->status.setText("Guardada. Instalación independiente, solo por el sistema autorizado."));}catch(Exception e){runOnUiThread(()->status.setText("No se pudo guardar el archivo"));}}).start();
    }
    private void stop(){LocalReceiver r=receiver;receiver=null;if(r!=null)r.close();if(status!=null){status.setText("Sesión cerrada");address.setText("");code.setText("");}}
    @Override protected void onPause(){stop();super.onPause();}
    @Override protected void onDestroy(){stop();super.onDestroy();}
}
