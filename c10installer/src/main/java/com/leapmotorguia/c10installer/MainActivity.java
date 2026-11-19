package com.leapmotorguia.c10installer;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import java.io.*;

/** Public PackageInstaller API: normal user confirmation, never bypasses OEM policy. */
public final class MainActivity extends Activity {
    private static final int PICK=21,EXPORT=22;
    private TextView output;
    private Button choose;
    private boolean busy;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff0c1721);LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(24,24,24,24);scroll.addView(col);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=35)scroll.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets s=in.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(s.left,s.top,s.right,s.bottom);return in;});
        add(col,"C10 Instalador diagnóstico · 1.0",26);
        add(col,"Herramienta pequeña, sin internet ni bibliotecas nativas. Debe instalarse primero por el procedimiento habitual. Si también es rechazada, no puede ejecutarse para saber el motivo.",17);
        add(col,"Selecciona el APK de C10 ControlPlus. Android pedirá confirmar. Solo podremos mostrar los códigos que el propio sistema devuelva. No cambia firmware, certificados ni protecciones del coche.",17);
        choose=button(col,"Seleccionar APK",()->select());
        button(col,"Actualizar resultado",()->refresh());
        button(col,"Guardar resultado TXT",()->{try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"C10-instalacion.txt"),EXPORT);}catch(RuntimeException e){output.setText("Selector para guardar no disponible. Puedes fotografiar el resultado.");}});
        output=add(col,"Sin intento de instalación en esta herramienta.",18);output.setTextIsSelectable(true);refresh();
    }
    private TextView add(LinearLayout p,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xffedf7f5);t.setPadding(10,12,10,12);p.addView(t);return t;}
    private Button button(LinearLayout p,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());p.addView(b);return b;}
    private void select(){
        if(busy)return;
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(this).setMessage("Android exige permitir instalaciones desde esta herramienta. Abre el ajuste oficial si está disponible; si LeapOS lo bloquea, no lo podemos sustituir.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Abrir ajuste",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(RuntimeException e){output.setText("El sistema no ofrece ese ajuste. No se puede iniciar esta vía.");}}).show();return;
        }
        try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),PICK);}catch(RuntimeException e){output.setText("El sistema no tiene selector de documentos. No se puede seleccionar un APK por esta vía.");}
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        if(req==PICK)install(data.getData());
        if(req==EXPORT)try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out!=null)out.write(output.getText().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){output.setText("No se pudo guardar el informe.");}
    }
    private void install(Uri uri){
        busy=true;choose.setEnabled(false);output.setText("Preparando sesión de instalación…");getSharedPreferences("install",0).edit().putString("report","Preparando sesión de instalación…").apply();
        new Thread(()->{
            PackageInstaller pi=getPackageManager().getPackageInstaller();int id=-1;boolean committed=false;
            try{
                PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                id=pi.createSession(params);
                try(PackageInstaller.Session session=pi.openSession(id)){
                    try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=session.openWrite("base.apk",0,-1)){
                        if(in==null)throw new IOException("no input");byte[] buffer=new byte[65536];int n;long bytes=0;
                        while((n=in.read(buffer))!=-1){bytes+=n;if(bytes>250L*1024*1024)throw new IOException("APK too large");out.write(buffer,0,n);}session.fsync(out);
                    }
                    Intent callback=new Intent(this,InstallResult.class).setAction(getPackageName()+".RESULT."+id);
                    int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;
                    PendingIntent result=PendingIntent.getBroadcast(this,id,callback,flags);session.commit(result.getIntentSender());committed=true;
                }
            }catch(Exception e){String msg="No se pudo iniciar la instalación: "+e.getClass().getSimpleName()+"\n"+String.valueOf(e.getMessage());getSharedPreferences("install",0).edit().putString("report",msg).apply();}
            finally{if(!committed&&id>=0)try{pi.abandonSession(id);}catch(RuntimeException ignored){}runOnUiThread(()->{busy=false;choose.setEnabled(true);refresh();});}
        },"c10-package-install").start();
    }
    private void refresh(){if(output!=null)output.setText("Android "+Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT+"\n"+getSharedPreferences("install",0).getString("report","Sin intentos. Esta herramienta no puede leer el fallo de una instalación realizada por otra app."));}
    @Override protected void onResume(){super.onResume();refresh();}
}
