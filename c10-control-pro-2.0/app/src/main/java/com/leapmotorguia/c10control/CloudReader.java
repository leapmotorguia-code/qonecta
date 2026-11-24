package com.leapmotorguia.c10control;
import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;
import java.util.*;
final class CloudReader {
 String read(String base,String token,String entities){
  try{
   URI u=new URI(base);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getRawUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||!(u.getPath().isEmpty()||"/".equals(u.getPath())))return "Introduce solo la URL HTTPS de tu Home Assistant.";
   if(token.isEmpty()||token.length()>4096||token.contains("\r")||token.contains("\n"))return "Token no válido.";
   String[] ids=entities.split(",");if(ids.length>16)return "Máximo 16 entidades por lectura.";
   String origin=new URI("https",null,u.getHost(),u.getPort(),null,null,null).toString();StringBuilder out=new StringBuilder("Home Assistant · lectura ").append(new Date()).append('\n');Set<String> seen=new HashSet<>();
   for(String raw:ids){String id=raw.trim();if(!id.matches("(sensor|binary_sensor)\\.[a-z0-9_]+")){out.append("ID inválido; solo sensor o binary_sensor.\n");continue;}if(!seen.add(id))continue;
    HttpsURLConnection c=(HttpsURLConnection)new URL(origin+"/api/states/"+id).openConnection();
    try{c.setRequestMethod("GET");c.setConnectTimeout(6000);c.setReadTimeout(6000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");int code=c.getResponseCode();if(code!=200){out.append(id).append(": HTTP ").append(code).append('\n');if(code==401||code==403||code==429)return out.toString();continue;}
     ByteArrayOutputStream buf=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] a=new byte[4096];int n;while((n=in.read(a))!=-1){if(buf.size()+n>65536)throw new IOException("too large");buf.write(a,0,n);}}
     JSONObject j=new JSONObject(buf.toString("UTF-8"));JSONObject at=j.optJSONObject("attributes");String state=j.optString("state","unknown"),unit=at==null?"":at.optString("unit_of_measurement","");out.append(id).append(": ").append(state).append(' ').append(unit).append("\nActualización: ").append(j.optString("last_updated","desconocida")).append('\n');
    }finally{c.disconnect();}
   }return out.toString();
  }catch(Exception e){return "Lectura no disponible: "+e.getClass().getSimpleName()+". Comprueba HTTPS, conexión y token.";}
 }
}