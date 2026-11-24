package com.leapmotorguia.c10control;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;
final class SpeedGraph extends View {
 static final class Point { final long ms;final float kmh;Point(long ms,float kmh){this.ms=ms;this.kmh=kmh;} }
 static void add(List<Point> a,long ms,float kmh){if(!Float.isFinite(kmh)||kmh<0||kmh>350||(!a.isEmpty()&&ms<=a.get(a.size()-1).ms))return;a.add(new Point(ms,kmh));while(!a.isEmpty()&&(ms-a.get(0).ms>120000||a.size()>1000))a.remove(0);}
 final Paint p=new Paint(3);final int bg,fg,accent;final boolean gauge;List<Point> samples=new ArrayList<>();Float speed;long now;
 SpeedGraph(Context c,int bg,int fg,int accent,boolean gauge){super(c);this.bg=bg;this.fg=fg;this.accent=accent;this.gauge=gauge;}
 void update(List<Point> points,Float s,long t){samples=new ArrayList<>(points);speed=s;now=t;invalidate();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density; c.drawColor(bg);p.setStyle(Paint.Style.FILL);p.setColor(fg);p.setTextSize(13*d);c.drawText(gauge?"VELOCIDAD GPS":"VELOCIDAD GPS · 120 s",12*d,20*d,p);
 if(gauge){float r=Math.min(w*.38f,h*.32f),x=w/2,y=h*.56f;RectF rect=new RectF(x-r,y-r,x+r,y+r);p.setStrokeWidth(12*d);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);p.setColor(0xff415166);c.drawArc(rect,145,250,false,p);if(speed!=null){p.setColor(accent);c.drawArc(rect,145,250*Math.min(1,speed/200f),false,p);}p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(fg);p.setTextSize(32*d);c.drawText(speed==null?"—":String.format(Locale.US,"%.0f",speed),x,y+8*d,p);p.setTextSize(13*d);c.drawText("km/h",x,y+27*d,p);p.setTextAlign(Paint.Align.LEFT);return;}
 float left=34*d,top=33*d,right=w-12*d,bottom=h-22*d;if(right<=left||bottom<=top)return;float max=50;for(Point q:samples)if(q.ms>=now-120000)max=Math.max(max,(float)Math.ceil(q.kmh/50)*50);
 p.setStrokeWidth(d);p.setTextSize(10*d);for(int i=0;i<=4;i++){float y=bottom-(bottom-top)*i/4;p.setColor(0xff415166);c.drawLine(left,y,right,y,p);p.setColor(fg);c.drawText(String.format(Locale.US,"%.0f",max*i/4),4*d,y,p);}
 p.setColor(accent);p.setStrokeWidth(2*d);Point last=null;for(Point q:samples){if(q.ms<now-120000||q.ms>now){last=null;continue;}float x=right-(right-left)*(now-q.ms)/120000f,y=bottom-(bottom-top)*q.kmh/max;if(last!=null&&q.ms-last.ms<=3500){float lx=right-(right-left)*(now-last.ms)/120000f,ly=bottom-(bottom-top)*last.kmh/max;c.drawLine(lx,ly,x,y,p);}c.drawCircle(x,y,1.5f*d,p);last=q;}p.setColor(fg);p.setTextSize(10*d);c.drawText("-120 s",left,h-5*d,p);c.drawText("ahora",right-30*d,h-5*d,p);
 }
}
