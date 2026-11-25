package es.leapmotorguia.c10plus;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** Native, read-only cockpit. No demo data, no vehicle commands. */
final class PremiumCockpit extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private static final int BG=0xff06131e, PANEL=0xff0c2230, EDGE=0xff224557, WHITE=0xffeaf7ff, MUTED=0xff9dbac9, TEAL=0xff27e3c1, BLUE=0xff59c8ff, GREEN=0xff9ce84b, AMBER=0xffffc779;
    private double speed=Double.NaN,soc=Double.NaN,power=Double.NaN,spent=Double.NaN,incoming=Double.NaN,km=Double.NaN,fuel=Double.NaN;
    private boolean partial;
    PremiumCockpit(Context c){super(c);setContentDescription("Cuadro digital de datos UNIT; no sustituye los instrumentos originales.");}
    void update(double v,double battery,double kw,double used,double in,double distance,double litres,boolean incomplete){
        speed=v>=0&&v<=350?v:Double.NaN;soc=battery>=0&&battery<=100?battery:Double.NaN;
        power=Double.isFinite(kw)?kw:Double.NaN;spent=used;incoming=in;km=distance;fuel=litres;partial=incomplete;
        invalidate();
    }
    private void box(Canvas c,float x,float y,float w,float h,int color){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,15,15,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(EDGE);p.setStrokeWidth(1);c.drawRoundRect(x,y,x+w,y+h,15,15,p);p.setStyle(Paint.Style.FILL);
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,float max,boolean center){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);
        while(p.measureText(s)>max&&p.getTextSize()>9)p.setTextSize(p.getTextSize()*.94f);
        p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);p.setTextAlign(Paint.Align.LEFT);
    }
    private String fmt(double d,int digits,String unit){return Double.isFinite(d)?String.format(Locale.getDefault(),"%."+digits+"f",d)+unit:"—";}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float width){p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(width);p.setColor(color);c.drawLine(x,y,x2,y2,p);p.setStyle(Paint.Style.FILL);}
    private void arc(Canvas c,float cx,float cy,float r,float start,float sweep,float width,int col){rect.set(cx-r,cy-r,cx+r,cy+r);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(col);p.setStrokeWidth(width);c.drawArc(rect,start,sweep,false,p);p.setStyle(Paint.Style.FILL);}
    private void tile(Canvas c,float x,float y,float w,float h,String title,String value,String hint,int color){
        box(c,x,y,w,h,PANEL);line(c,x+14,y+13,x+14,y+h-13,color,3);
        text(c,title,x+25,y+22,14,MUTED,false,w-34,false);
        text(c,value,x+25,y+51,27,color,true,w-34,false);
        text(c,hint,x+25,y+h-11,12,MUTED,false,w-34,false);
    }
    private void drawCar(Canvas c,float x,float y,float w,float h){
        // Original vector silhouette; never implies vehicle status.
        p.setStyle(Paint.Style.FILL);p.setColor(0xff142e3e);c.drawRoundRect(x,y+h*.44f,x+w,y+h*.81f,12,12,p);
        Path roof=new Path();roof.moveTo(x+w*.15f,y+h*.46f);roof.lineTo(x+w*.31f,y+h*.16f);roof.lineTo(x+w*.69f,y+h*.16f);roof.lineTo(x+w*.84f,y+h*.46f);roof.close();p.setColor(0xff31586c);c.drawPath(roof,p);
        p.setColor(BG);c.drawCircle(x+w*.21f,y+h*.79f,h*.15f,p);c.drawCircle(x+w*.79f,y+h*.79f,h*.15f,p);
        line(c,x+w*.08f,y+h*.52f,x+w*.18f,y+h*.52f,BLUE,3);line(c,x+w*.85f,y+h*.52f,x+w*.94f,y+h*.52f,0xffff7068,3);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();if(w<2||h<2)return;c.drawColor(BG);
        // Responsive coordinate system keeps all labels and gauges within bounds.
        boolean compact=w/h<1.3f;float dw=compact?620:1000,dh=compact?650:510;
        float scale=Math.min(w/dw,h/dh);c.save();c.translate((w-dw*scale)/2,(h-dh*scale)/2);c.scale(scale,scale);
        float cx=dw/2,cy=compact?189:181,r=compact?135:143;
        box(c,8,8,dw-16,compact?357:344,0xff0a1d2a);
        text(c,"TELEMETRÍA · UNIT",24,32,16,TEAL,true,dw-48,false);
        arc(c,cx,cy,r,140,260,12,EDGE);
        if(Double.isFinite(speed)){float f=(float)Math.min(1,speed/200);if(f>0)arc(c,cx,cy,r,140,260*f,12,TEAL);}
        for(int i=0;i<=10;i++){
            double angle=Math.toRadians(140+i*26);float x1=cx+(r-17)*(float)Math.cos(angle),y1=cy+(r-17)*(float)Math.sin(angle);
            float x2=cx+(r-27)*(float)Math.cos(angle),y2=cy+(r-27)*(float)Math.sin(angle);
            line(c,x1,y1,x2,y2,MUTED,1.7f);
            text(c,Integer.toString(i*20),cx+(r-44)*(float)Math.cos(angle),cy+(r-44)*(float)Math.sin(angle)+4,12,MUTED,false,38,true);
        }
        if(Double.isFinite(speed)){double a=Math.toRadians(140+260*Math.min(1,speed/200));line(c,cx+(r-28)*(float)Math.cos(a),cy+(r-28)*(float)Math.sin(a),cx+(r+4)*(float)Math.cos(a),cy+(r+4)*(float)Math.sin(a),WHITE,4);}
        text(c,fmt(speed,0,""),cx,cy+9,78,WHITE,true,r*1.32f,true);
        text(c,"km/h",cx,cy+36,18,MUTED,false,90,true);
        text(c,Double.isFinite(speed)?"LECTURA UNIT":"SIN LECTURA",cx,cy+57,12,Double.isFinite(speed)?TEAL:MUTED,true,150,true);
        float side=compact?128:220;
        float lx=compact?20:32,rx=dw-side-(compact?20:32);
        tile(c,lx,66,side,84,"POTENCIA",fmt(power,1," kW"),"Batería · signo UNIT",AMBER);
        tile(c,rx,66,side,84,"BATERÍA",fmt(soc,0," %"),"Estado de carga",GREEN);
        if(Double.isFinite(soc)){line(c,rx+23,160,rx+side-12,160,EDGE,4);if(soc>0)line(c,rx+23,160,rx+23+(side-35)*(float)(soc/100),160,GREEN,4);}
        tile(c,lx,178,side,83,"ENTRADA",fmt(incoming,2," kWh"),"No solo frenada",TEAL);
        tile(c,rx,178,side,83,"COMBUSTIBLE",fmt(fuel,1," L"),"Restante · si disponible",BLUE);
        drawCar(c,cx-70,cy+68,140,58);
        text(c,"Datos originales del vehículo · no sustituye al cuadro OEM",cx,compact?352:339,12,MUTED,false,dw-46,true);
        float y=compact?377:365;float cell=(dw-40)/3f;
        double net=Double.isFinite(spent)&&Double.isFinite(incoming)?spent-incoming:Double.NaN;
        double avg=Double.isFinite(net)&&km>0&&!partial?net*100/km:Double.NaN;
        tile(c,8,y,cell,compact?103:105,"CONSUMO BRUTO",fmt(spent,2," kWh"),partial?"Medición parcial":"Energía de salida",AMBER);
        tile(c,20+cell,y,cell,compact?103:105,"BALANCE NETO",fmt(net,2," kWh"),"Salida menos entrada",TEAL);
        tile(c,32+cell*2,y,cell,compact?103:105,"VIAJE",fmt(km,1," km"),"Distancia registrada",BLUE);
        if(compact){tile(c,8,492,(dw-28)/2,100,"MEDIA NETA",fmt(avg,1," kWh/100 km"),"Si distancia y energía fiables",GREEN);tile(c,20+(dw-28)/2,492,(dw-28)/2,100,"CLIMA / AUTONOMÍA","—","Sin fuente confirmada",MUTED);}
        else{text(c,"Media neta: "+fmt(avg,1," kWh/100 km")+"   ·   Clima / autonomía: sin fuente confirmada",20,496,14,MUTED,false,dw-40,false);}
        c.restore();
    }
}
