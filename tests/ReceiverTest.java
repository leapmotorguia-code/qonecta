package es.drivecore.connect;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
public class ReceiverTest {
 static int request(LocalReceiver r,String code,byte[] bytes)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(r.url()+"upload").openConnection();
  c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(3000);c.setReadTimeout(5000);c.setFixedLengthStreamingMode(bytes.length);
  c.setRequestProperty("Content-Type","application/octet-stream");c.setRequestProperty("X-DriveCore-Code",code);
  try(OutputStream o=c.getOutputStream()){o.write(bytes);}int status=c.getResponseCode();c.disconnect();return status;
 }
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("connect-test").toFile();
  CountDownLatch received=new CountDownLatch(1);
  byte[] data={80,75,3,4,5,6};
  try(LocalReceiver r=new LocalReceiver(InetAddress.getLoopbackAddress() instanceof Inet4Address?InetAddress.getLoopbackAddress():InetAddress.getByName("127.0.0.1"),0,dir,"ok".getBytes(),new LocalReceiver.Listener(){public void message(String s){}public void received(File f,long n,String h){received.countDown();}})){
   r.start();if(request(r,"invalid",data)!=403)throw new AssertionError("bad code");
   if(request(r,r.code(),data)!=200)throw new AssertionError("transfer");
   if(!received.await(3,TimeUnit.SECONDS))throw new AssertionError("callback");
   if(!java.util.Arrays.equals(Files.readAllBytes(new File(dir,"received.apk").toPath()),data))throw new AssertionError("bytes");
  }finally{new File(dir,"received.apk").delete();dir.delete();}
  System.out.println("PASS: rejected invalid token; byte-exact authorized upload");
 }
}
