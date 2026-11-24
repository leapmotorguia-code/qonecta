package com.leapmotorguia.c10control;

import java.util.Locale;

/** Cálculos explícitos sobre datos proporcionados por la persona. Nunca se inventa recuperación del BMS. */
final class EnergyLedger {
    final double socInitial,socFinal,km,grossKwh,recoveredKwh,fuelL,usableKwh;
    EnergyLedger(double si,double sf,double km,double gross,double rec,double fuel,double usable){
        this.socInitial=si;this.socFinal=sf;this.km=km;this.grossKwh=gross;
        this.recoveredKwh=rec;this.fuelL=fuel;this.usableKwh=usable;
    }
    static double parse(String s){try{if(s==null||s.trim().isEmpty())return Double.NaN;
        return Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    boolean socValid(){return validPct(socInitial)&&validPct(socFinal)&&km>0&&usableKwh>0;}
    private static boolean validPct(double n){return n>=0 && n<=100;}
    boolean manualValid(){return grossKwh>=0&&recoveredKwh>=0;}
    double batteryNet(){return (socInitial-socFinal)/100*usableKwh;}
    double manualNet(){return grossKwh-recoveredKwh;}
    double netPer100(){return socValid()?batteryNet()/km*100:Double.NaN;}
    double fuelPer100(){return fuelL>=0&&km>0?fuelL/km*100:Double.NaN;}
    double recoverPercent(){return manualValid()&&grossKwh>0?100*recoveredKwh/grossKwh:Double.NaN;}
    static String fmt(double d,String units){return Double.isFinite(d)?String.format(Locale.forLanguageTag("es-ES"),"%.2f %s",d,units):"—";}
}
