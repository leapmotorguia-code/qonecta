import pathlib,sys,xml.etree.ElementTree as ET,shutil
root=pathlib.Path(sys.argv[1]);p=root/"receiver-combined";src=pathlib.Path(sys.argv[2])
def edit(rel,old,new):
 f=p/rel;s=f.read_text()
 if s.count(old)!=1:raise RuntimeError("Expected exactly one match: "+rel+" "+old)
 f.write_text(s.replace(old,new,1))
edit("app/build.gradle.kts","versionCode = 10","versionCode = 11")
edit("app/build.gradle.kts",'versionName = "6.4-fusion-premium-0.9"','versionName = "1.0-premium-connect"')
f=p/"cockpit/build.gradle"
s=f.read_text();f.write_text(s+"\ndependencies { implementation 'androidx.core:core:1.15.0' }\n")
d=p/"cockpit/src/main"; dest=d/"java/es/drivecore/connect";dest.mkdir(parents=True,exist_ok=True)
for name in ("LocalReceiver.java","ConnectActivity.java"):shutil.copy2(src/name,dest/name)
shutil.copy2(src/"connect.html",d/"assets/connect.html")
(d/"res/xml/connect_file_paths.xml").write_text('<paths xmlns:android="http://schemas.android.com/apk/res/android"><files-path name="inbox" path="connect-inbox/"/></paths>')
A="{http://schemas.android.com/apk/res/android}";ET.register_namespace("android",A[1:-1])
f=d/"AndroidManifest.xml";doc=ET.parse(f);r=doc.getroot();app=r.find("application")
ET.SubElement(r,"uses-permission",{A+"name":"android.permission.REQUEST_INSTALL_PACKAGES"})
ET.SubElement(app,"activity",{A+"name":"es.drivecore.connect.ConnectActivity",A+"exported":"false",A+"screenOrientation":"sensorLandscape",A+"theme":"@style/DriveTheme"})
pr=ET.SubElement(app,"provider",{A+"name":"androidx.core.content.FileProvider",A+"authorities":"${applicationId}.connectfiles",A+"exported":"false",A+"grantUriPermissions":"true"})
# A subclass avoids manifest merging with upstream's FileProvider.
pr.set(A+"name","es.drivecore.connect.ConnectFileProvider")
(dest/"ConnectFileProvider.java").write_text("package es.drivecore.connect; public class ConnectFileProvider extends androidx.core.content.FileProvider {}")
ET.SubElement(pr,"meta-data",{A+"name":"android.support.FILE_PROVIDER_PATHS",A+"resource":"@xml/connect_file_paths"})
doc.write(f,encoding="utf-8",xml_declaration=True)
m="cockpit/src/main/java/es/drivecore/cockpit/MainActivity.kt"
edit(m,'\"Ajustes\" to {pickMode(\"Diagnóstico\")},\"Vistas\"','\"Ajustes\" to {pickMode(\"Diagnóstico\")},\"Actualizar\" to {openConnect()},\"Vistas\"')
edit(m,'    private fun projection(){','''    private fun openConnect(){
        AlertDialog.Builder(this).setTitle("DriveCore Connect").setMessage("Con el vehículo estacionado, conecta el móvil y el coche a una red privada de confianza. El receptor solo funciona mientras su pantalla está abierta.").setPositiveButton("Abrir receptor"){_,_->
            radio.stop();stopVideo()
            startActivity(Intent(this,es.drivecore.connect.ConnectActivity::class.java))
        }.setNegativeButton("Cancelar",null).show()
    }
    private fun projection(){''')
print("Premium 1.0: integrated local receiver, own-package update check, preserved cockpit.")
