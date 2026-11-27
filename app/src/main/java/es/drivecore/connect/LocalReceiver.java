package es.drivecore.connect;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Single-session, user-started LAN file receiver. No installer or car commands. */
public final class LocalReceiver implements AutoCloseable {
    public interface Listener { void message(String text); void received(File file, long size, String hash); }
    public static final long MAX=150L*1024*1024;
    private final ServerSocket server;
    private final File dir;
    private final byte[] page;
    private final Listener listener;
    private final String code, host;
    private final long deadline=System.nanoTime()+300_000_000_000L;
    private final AtomicBoolean closed=new AtomicBoolean();
    private volatile Socket active;
    private int failed;
    public LocalReceiver(InetAddress bind,int port,File dir,byte[] page,Listener listener)throws IOException {
        this.dir=dir;this.page=page;this.listener=listener;
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("No storage");
        byte[] random=new byte[16];new SecureRandom().nextBytes(random);code=hex(random);
        server=new ServerSocket();server.bind(new InetSocketAddress(bind,port),3);server.setSoTimeout(1000);
        host=bind.getHostAddress()+":"+server.getLocalPort();
    }
    public String url(){return "http://"+host+"/";}
    public String code(){return code;}
    public void start(){new Thread(this::loop,"drivecore-receiver").start();}
    private boolean expired(){return System.nanoTime()-deadline>=0;}
    private void loop(){
        try {
            while(!closed.get()&&!expired()){
                try(Socket socket=server.accept()){
                    active=socket;socket.setSoTimeout(5000);
                    if(!closed.get())try{handle(socket);}catch(IOException e){if(!closed.get())listener.message("Solicitud interrumpida");}
                }catch(SocketTimeoutException ignored){}finally{active=null;}
            }
        }catch(IOException ignored){}finally{close();listener.message("Sesión cerrada");}
    }
    private String line(InputStream in)throws IOException{
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        while(b.size()<4096){int x=in.read();if(x<0)throw new EOFException();if(x==10){byte[] a=b.toByteArray();if(a.length==0||a[a.length-1]!=13)throw new IOException("CRLF");return new String(a,0,a.length-1,StandardCharsets.US_ASCII);}b.write(x);}
        throw new IOException("Header too long");
    }
    private void handle(Socket s)throws IOException{
        InputStream in=new BufferedInputStream(s.getInputStream());OutputStream out=s.getOutputStream();
        String[] req=line(in).split(" ");if(req.length!=3||!req[2].equals("HTTP/1.1")){reply(out,400,"Solicitud incorrecta");return;}
        Map<String,String> h=new HashMap<>();
        for(int i=0;;i++){if(i>=32)throw new IOException("Too many headers");String l=line(in);if(l.isEmpty())break;int colon=l.indexOf(':');if(colon<1)throw new IOException("Bad header");String k=l.substring(0,colon).toLowerCase(Locale.ROOT);if(h.put(k,l.substring(colon+1).trim())!=null)throw new IOException("Duplicate header");}
        if(!host.equals(h.get("host"))||h.containsKey("transfer-encoding")){reply(out,400,"Host o formato no válido");return;}
        if(req[0].equals("GET")&&req[1].equals("/")){send(out,200,"text/html; charset=utf-8",page);return;}
        if(!req[0].equals("POST")||!req[1].equals("/upload")){reply(out,404,"No disponible");return;}
        if(h.containsKey("origin")&&!h.get("origin").equals("http://"+host)){reply(out,403,"Origen no autorizado");return;}
        String supplied=h.getOrDefault("x-drivecore-code","");
        if(!MessageDigest.isEqual(code.getBytes(StandardCharsets.US_ASCII),supplied.getBytes(StandardCharsets.US_ASCII))){
            failed++;reply(out,403,"Código incorrecto");if(failed>=5)close();return;
        }
        if(!"application/octet-stream".equals(h.get("content-type"))){reply(out,415,"Formato no permitido");return;}
        long n;try{n=Long.parseLong(h.getOrDefault("content-length","-1"));}catch(NumberFormatException e){n=-1;}
        if(n<4||n>MAX){reply(out,413,"Tamaño no permitido (máximo 150 MiB)");return;}
        if(dir.getUsableSpace()<n+16*1024*1024L){reply(out,507,"Espacio insuficiente");return;}
        if(new File(dir,"received.apk").exists()){reply(out,409,"Ya hay un archivo pendiente");return;}
        File temp=new File(dir,"upload.part");temp.delete();
        try{
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(FileOutputStream f=new FileOutputStream(temp)){
                byte[] b=new byte[32768];long left=n;boolean first=true;
                while(left>0){if(expired()||closed.get())throw new IOException("Session closed");int count=in.read(b,0,(int)Math.min(b.length,left));if(count<0)throw new EOFException();
                    if(first){ // Read a complete ZIP header even if TCP fragments the first bytes.
                        while(count<4){int extra=in.read(b,count,4-count);if(extra<0)throw new EOFException();count+=extra;}
                        if(b[0]!=80||b[1]!=75||b[2]!=3||b[3]!=4)throw new IOException("Not ZIP/APK");first=false;
                    }
                    f.write(b,0,count);md.update(b,0,count);left-=count;
                }
                f.getFD().sync();
            }
            File done=new File(dir,"received.apk");if(!temp.renameTo(done))throw new IOException("Save failed");
            String hash=hex(md.digest());reply(out,200,"Recibido para revisión. SHA-256: "+hash);
            listener.received(done,n,hash);close();
        }catch(NoSuchAlgorithmException e){throw new IOException(e);}finally{temp.delete();}
    }
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
    private void reply(OutputStream out,int status,String text)throws IOException{send(out,status,"text/plain; charset=utf-8",text.getBytes(StandardCharsets.UTF_8));}
    private void send(OutputStream out,int status,String type,byte[] data)throws IOException{
        String header="HTTP/1.1 "+status+(status==200?" OK":" Error")+"\r\nContent-Type: "+type+"\r\nContent-Length: "+data.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; form-action 'none'; frame-ancestors 'none'\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII));out.write(data);out.flush();
    }
    @Override public void close(){if(closed.compareAndSet(false,true)){try{server.close();}catch(IOException ignored){}try{if(active!=null)active.close();}catch(IOException ignored){}}}
}
