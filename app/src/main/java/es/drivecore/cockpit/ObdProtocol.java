package es.drivecore.cockpit;
import java.util.*;
/** SAE J1979 mode 01, strictly read-only, no manufacturer-specific commands. */
public final class ObdProtocol {
 public static final String[] QUERIES={"010D","012F","010C","0105","0146"};
 public static Double decode(String command,String response) {
  if(!Arrays.asList(QUERIES).contains(command)||response==null)return null;
  String pid=command.substring(2); Double found=null;
  for(String line:response.toUpperCase(Locale.ROOT).split("[\\r\\n>]+")){
   String s=line.replaceAll("\\s+",""); if(!s.matches("[0-9A-F]+")||!s.startsWith("41"+pid))continue;
   int n=pid.equals("0C")?8:6; if(s.length()!=n)continue;
   int a=Integer.parseInt(s.substring(4,6),16); double v;
   switch(pid){case "0D":v=a;break;case "2F":v=a*100.0/255;break;case "0C":v=(a*256+Integer.parseInt(s.substring(6,8),16))/4.0;break;default:v=a-40;}
   if(found!=null&&Math.abs(found-v)>0.001)return null; found=v;
  }return found;
 }
}
