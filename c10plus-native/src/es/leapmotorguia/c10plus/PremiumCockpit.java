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
    private Bitmap carArt;
    PremiumCockpit(Context c){super(c);try(java.io.InputStream in=c.getAssets().open("c10-premium-car.png")){carArt=BitmapFactory.decodeStream(in);}catch(Exception ignored){}setContentDescription("Cuadro digital de datos UNIT; no sustituye los instrumentos originales.");}
    void update(double v,double battery,double kw,double used,double in,double distance,double litres,boolean incomplete){
        speed=v>=0&&v<=350?v:Double.NaN;soc=battery>=0&&battery<=100?battery:Double.NaN;
        power=Double.isFinite(kw)?kw:Double.NaN;spent=used;incoming=in;km=distance;fuel=litres;partial=incomplete;
        invalidate();
    }
    private void box(Canvas c,float x,float y,float w,float h,int color){
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(x,y,x+w,y+h,color,0xff061923,Shader.TileMode.CLAMP));c.drawRoundRect(x,y,x+w,y+h,15,15,p);p.setShader(null);
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
        if(carArt!=null){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setFilterBitmap(true);c.drawBitmap(carArt,null,new RectF(x,y,x+w,y+h),p);return;}
        c.save();c.translate(x,y);c.scale(w/260,h/104);
        p.setShader(new LinearGradient(0,25,0,88,new int[]{0xffaac2ce,0xff4a697e,0xff152c3a},null,Shader.TileMode.CLAMP));
        Path body=new Path();body.moveTo(8,68);body.lineTo(16,48);body.lineTo(50,38);body.lineTo(80,13);body.quadTo(93,5,116,6);body.lineTo(177,9);body.quadTo(197,14,213,37);body.lineTo(240,43);body.quadTo(255,48,255,72);body.lineTo(243,84);body.lineTo(15,84);body.close();c.drawPath(body,p);p.setShader(null);
        Path glass=new Path();glass.moveTo(58,36);glass.lineTo(84,16);glass.lineTo(126,12);glass.lineTo(174,15);glass.lineTo(204,37);glass.close();p.setShader(new LinearGradient(60,12,180,38,0xff476a7f,0xff071825,Shader.TileMode.CLAMP));c.drawPath(glass,p);p.setShader(null);
        line(c,125,13,125,38,0xffa4bdc9,2);line(c,173,15,177,39,0xff879fac,2);line(c,20,49,238,49,0xff92aeb9,1.4f);
        line(c,86,48,82,77,0xff274758,1);line(c,170,48,170,77,0xff274758,1);
        line(c,16,53,44,49,BLUE,3);line(c,214,49,248,54,0xffff4d61,4);
        for(int i=0;i<2;i++){float wx=i==0?53:205;p.setColor(0xff030b13);c.drawCircle(wx,80,20,p);p.setColor(0xff708b9a);c.drawCircle(wx,80,12,p);p.setColor(0xff1c3444);c.drawCircle(wx,80,8,p);for(int j=0;j<5;j++){double a=j*Math.PI*2/5;line(c,wx,80,wx+10*(float)Math.cos(a),80+10*(float)Math.sin(a),0xffa1bccb,1.6f);}}
        c.restore();
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();if(w<2||h<2)return;c.drawColor(BG);
        boolean compact=w/h<.95f;float dw=compact?640:820,dh=compact?760:650;
        float scale=Math.min(w/dw,h/dh);c.save();c.translate((w-dw*scale)/2,(h-dh*scale)/2);c.scale(scale,scale);
        float cx=dw/2,cy=184,r=154;float lower=compact?428:400;
        box(c,7,7,dw-14,lower-14,0xff0b2636);
        text(c,"TELEMETRÍA DIGITAL",21,29,16,TEAL,true,dw-42,false);
        text(c,"UNIT · lectura real",dw-112,29,12,MUTED,false,150,true);
        arc(c,cx,cy,r+13,137,266,1.5f,0xff327b90);
        arc(c,cx,cy,r,137,266,13,EDGE);
        if(Double.isFinite(speed)){float f=(float)Math.min(1,speed/200);if(f>0){
          arc(c,cx,cy,r,137,266*f,23,0xff115353);rect.set(cx-r,cy-r,cx+r,cy+r);
          p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(13);p.setStrokeCap(Paint.Cap.ROUND);
          SweepGradient g=new SweepGradient(cx,cy,new int[]{GREEN,TEAL,BLUE,BLUE},new float[]{0f,.35f,.75f,1f});Matrix m=new Matrix();m.setRotate(137,cx,cy);g.setLocalMatrix(m);p.setShader(g);c.drawArc(rect,137,266*f,false,p);p.setShader(null);p.setStyle(Paint.Style.FILL);
        }}
        for(int i=0;i<=40;i++){double a=Math.toRadians(137+i*6.65);float ri=i%4==0?r-28:r-19;line(c,cx+(r-12)*(float)Math.cos(a),cy+(r-12)*(float)Math.sin(a),cx+ri*(float)Math.cos(a),cy+ri*(float)Math.sin(a),i%4==0?MUTED:EDGE,i%4==0?2:1);}
        for(int i=0;i<=10;i++){double a=Math.toRadians(137+i*26.6);text(c,""+(i*20),cx+(r-44)*(float)Math.cos(a),cy+(r-44)*(float)Math.sin(a)+4,14,WHITE,false,38,true);}
        if(Double.isFinite(speed)){double a=Math.toRadians(137+266*Math.min(1,speed/200));line(c,cx+(r-55)*(float)Math.cos(a),cy+(r-55)*(float)Math.sin(a),cx+(r+5)*(float)Math.cos(a),cy+(r+5)*(float)Math.sin(a),TEAL,9);line(c,cx+(r-55)*(float)Math.cos(a),cy+(r-55)*(float)Math.sin(a),cx+(r+5)*(float)Math.cos(a),cy+(r+5)*(float)Math.sin(a),WHITE,3);}
        text(c,fmt(speed,0,""),cx,cy+12,86,WHITE,true,178,true);
        text(c,"km/h",cx,cy+39,20,MUTED,false,90,true);
        text(c,Double.isFinite(speed)?"VELOCIDAD UNIT":"SIN LECTURA",cx,cy+61,12,Double.isFinite(speed)?TEAL:MUTED,true,150,true);
        float side=(dw-340)/2-18,lx=16,rx=dw-16-side;
        tile(c,lx,61,side,92,"POTENCIA",fmt(Double.isFinite(power)&&power>=0?power:Double.NaN,1," kW"),"Salida de batería",GREEN);
        tile(c,rx,61,side,92,"POTENCIA ENTRANTE",fmt(Double.isFinite(power)&&power<0?-power:Double.NaN,1," kW"),"No solo frenada",TEAL);
        tile(c,lx,167,side,92,"BATERÍA",fmt(soc,0," %"),"Autonomía: —",GREEN);
        if(Double.isFinite(soc)){line(c,lx+25,250,lx+side-14,250,EDGE,3);if(soc>0)line(c,lx+25,250,lx+25+(side-39)*(float)(soc/100),250,GREEN,3);}
        tile(c,rx,167,side,92,"COMBUSTIBLE",fmt(fuel,1," L"),"Nivel · si disponible",AMBER);
        p.setShader(new RadialGradient(cx,328,150,new int[]{0xff174e50,0xff0a1d2a},null,Shader.TileMode.CLAMP));c.drawOval(cx-150,301,cx+150,351,p);p.setShader(null);
        rect.set(cx-145,311,cx+145,345);p.setStyle(Paint.Style.STROKE);p.setColor(TEAL);p.setStrokeWidth(1.5f);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);
        drawCar(c,cx-118,247,236,114);
        text(c,"FLUJO DE ENERGÍA",cx,370,14,TEAL,true,240,true);
        String flow=Double.isFinite(power)?power>0?"Batería → sistema eléctrico":power<0?"Entrada → batería":"Sin intercambio medido":"Sin potencia disponible";
        text(c,flow,cx,388,13,MUTED,false,dw-34,true);
        double net=Double.isFinite(spent)&&Double.isFinite(incoming)?spent-incoming:Double.NaN;
        double avg=Double.isFinite(net)&&km>0&&!partial?net*100/km:Double.NaN;
        float cell=(dw-40)/3f, y=lower, th=compact?91:80;
        tile(c,8,y,cell,th,"CONSUMO BRUTO",fmt(spent,2," kWh"),partial?"Medición parcial":"Salida medida",AMBER);
        tile(c,20+cell,y,cell,th,"CONSUMO MEDIO",fmt(avg,1," kWh/100 km"),"Neto · viaje",GREEN);
        tile(c,32+cell*2,y,cell,th,"ENERGÍA ENTRANTE",fmt(incoming,2," kWh"),"Incluye posible generador",TEAL);
        y+=th+9;
        tile(c,8,y,cell,th,"VIAJE ACTUAL",fmt(km,1," km"),"Distancia medida",BLUE);
        tile(c,20+cell,y,cell,th,"BALANCE NETO",fmt(net,2," kWh"),"Salida menos entrada",TEAL);
        tile(c,32+cell*2,y,cell,th,"CLIMA / EXTERIOR","—","Sin fuente confirmada",MUTED);
        y+=th+9;
        box(c,8,y,dw-16,54,PANEL);
        text(c,"ESTADO DEL VEHÍCULO",23,y+20,14,TEAL,true,dw*.47f,false);
        text(c,"Puertas · luces · modo: —",23,y+40,14,MUTED,false,dw*.53f,false);
        text(c,"OBD2 · sin conexión",dw*.62f,y+31,13,MUTED,false,dw*.35f,false);
        c.restore();
    }
}
