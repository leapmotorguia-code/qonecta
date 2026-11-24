package com.leapmotorguia.c10control;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Interface creada desde cero para la pantalla panorámica del coche. */
final class Ui {
    static final int BG = 0xff08151e;
    static final int NAV = 0xff0d202c;
    static final int PANEL = 0xff132936;
    static final int BORDER = 0xff274855;
    static final int TEXT = 0xffeaf7f8;
    static final int DIM = 0xffadc4cc;
    static final int MINT = 0xff56e3c2;
    static final int YELLOW = 0xfff1c17a;
    static final int RED = 0xffff9f96;
    private Ui() {}
    static int dp(Context c, int n) { return (int) (n * c.getResources().getDisplayMetrics().density + .5f); }
    static GradientDrawable bg(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(1, stroke);
        return d;
    }
    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView v = new TextView(c); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        v.setLineSpacing(2, 1.1f);
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return v;
    }
    static LinearLayout col(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    static LinearLayout.LayoutParams match(int h) { return new LinearLayout.LayoutParams(-1, h); }
    static TextView title(Context c, String s) {
        TextView t = text(c,s,27,TEXT,true); t.setPadding(0,0,0,dp(c,7)); return t;
    }
    static TextView caption(Context c, String s) {
        TextView t = text(c,s,14,DIM,false); t.setPadding(0,0,0,dp(c,11)); return t;
    }
    static TextView note(Context c,String s) {
        TextView t = text(c,s,15,YELLOW,false); t.setPadding(dp(c,15),dp(c,12),dp(c,15),dp(c,12));
        t.setBackground(bg(0xff382e25,dp(c,13),0)); return t;
    }
    static LinearLayout card(Context c, LinearLayout root, String title) {
        LinearLayout body=col(c); body.setPadding(dp(c,18),dp(c,14),dp(c,18),dp(c,14));
        body.setBackground(bg(PANEL,dp(c,15),BORDER));
        LinearLayout.LayoutParams p=match(-2); p.bottomMargin=dp(c,12); root.addView(body,p);
        if(title!=null){TextView t=text(c,title,16,MINT,true);t.setPadding(0,0,0,dp(c,10));body.addView(t);}
        return body;
    }
    static Button button(Context c,String label,boolean prominent) {
        Button b=new Button(c); b.setAllCaps(false);b.setText(label); b.setTextSize(15);
        b.setTextColor(prominent ? 0xff06251d : TEXT);
        b.setBackground(bg(prominent ? MINT : 0xff1d3a48,dp(c,11),prominent ? 0 : BORDER));
        b.setPadding(dp(c,15),dp(c,8),dp(c,15),dp(c,8));b.setMinimumHeight(dp(c,47));return b;
    }
    static EditText number(Context c,String hint) {
        EditText e=new EditText(c);e.setSingleLine(true); e.setTextColor(TEXT);e.setHintTextColor(DIM);
        e.setTextSize(18);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setHint(hint); e.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));
        e.setBackground(bg(0xff0b1e29,dp(c,9),BORDER));return e;
    }
    static void textRow(Context c,LinearLayout target,String label,String value, int color) {
        LinearLayout line=row(c);
        TextView k=text(c,label,15,DIM,false);
        LinearLayout.LayoutParams kp =new LinearLayout.LayoutParams(0,-2,1f);line.addView(k,kp);
        TextView v=text(c,value,16,color,true);v.setGravity(Gravity.END);line.addView(v);
        line.setPadding(0,dp(c,8),0,dp(c,8));target.addView(line);
    }
    static void smallGap(Context c,LinearLayout l) { View v=new View(c);l.addView(v,new LinearLayout.LayoutParams(1,dp(c,9))); }
    static ScrollView page(Context c,LinearLayout host) {
        host.removeAllViews();
        ScrollView scroll =new ScrollView(c);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout col=col(c);col.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,28));
        scroll.addView(col,new ScrollView.LayoutParams(-1,-2));
        host.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        return scroll;
    }
    static LinearLayout content(ScrollView scroll) {return (LinearLayout)scroll.getChildAt(0);}
    static void field(Context c,LinearLayout host,String label,EditText e,String value) {
        host.addView(text(c,label,14,DIM,false));
        e.setText(value);
        LinearLayout.LayoutParams p=match(dp(c,50));p.bottomMargin=dp(c,12);host.addView(e,p);
    }
}
