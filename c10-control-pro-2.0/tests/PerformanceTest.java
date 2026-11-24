import com.leapmotorguia.c10control.Performance;
public class PerformanceTest {public static void main(String[] args){
 Performance p=new Performance(80,120); for(int i=0;i<=8;i++)p.sample(i*1000,80); if(p.result!=null)throw new AssertionError();p.sample(9000,90);p.sample(10000,110);p.sample(11000,130);if(p.result==null||p.result<1||p.result>3)throw new AssertionError(p.result);
 Performance q=new Performance(0,100);q.sample(0,0);q.sample(1000,20);q.sample(2000,60);q.sample(3000,110);if(q.result==null)throw new AssertionError();
 Performance r=new Performance(80,120);r.sample(0,80);r.sample(5000,120);if(r.result!=null)throw new AssertionError();System.out.println("3 pruebas GPS correctas");}}