package com.leapmotorguia.c10control;

import android.content.Context;
import android.content.SharedPreferences;

/** Datos y preferencias de C10 Control Pro, sin servicios de terceros. */
final class VehicleStore {
    static final String[] NAMES={"C10 REEV", "C10 BEV", "C10 Pro Max", "B10", "T03"};
    static final double[] CAPACITIES={28.4,69.9,81.9,67.1,37.3};
    static final double[] USABLE={26.5,67.0,79.0,64.0,36.0};
    final SharedPreferences p;
    VehicleStore(Context c){p=c.getSharedPreferences("c10control_config",Context.MODE_PRIVATE);}
    int model(){return Math.min(NAMES.length-1,Math.max(0,p.getInt("model",0)));}
    void model(int i){p.edit().putInt("model",Math.max(0,Math.min(NAMES.length-1,i))).apply();}
    String modelName(){return NAMES[model()];}
    double nominal(){return CAPACITIES[model()];}
    double usable(){return p.getFloat("usable_kwh",(float)USABLE[model()]);}
    void usable(double v){p.edit().putFloat("usable_kwh",(float)v).apply();}
    void resetUsable(){p.edit().remove("usable_kwh").apply();}
    boolean isReev(){return model()==0;}
    void setString(String k,String v){p.edit().putString(k,v).apply();}
    String getString(String k,String def){return p.getString(k,def);}
    void setLong(String k,long val){p.edit().putLong(k,val).apply();}
    long getLong(String k,long def){return p.getLong(k,def);}
}
