package com.leapmotorguia.c10nativa;

/** Strict standard SAE PID decoder. No guessed manufacturer-specific measurements. */
public final class ObdValues {
    private ObdValues(){}
    public static Double decode(String pid,String response) {
        if(response==null||!pid.matches("0[45CD]"))return null;
        String hex=response.toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+","");
        int i=hex.indexOf("41"+pid); if(i<0)return null;
        int bytes=pid.equals("0C")?2:1;i+=4;
        if(hex.length()<i+bytes*2)return null;
        String raw=hex.substring(i,i+bytes*2);if(!raw.matches("[0-9A-F]+"))return null;
        int a=Integer.parseInt(raw.substring(0,2),16);
        if(pid.equals("0C"))return (a*256+Integer.parseInt(raw.substring(2,4),16))/4.0;
        if(pid.equals("05"))return a-40.0;
        if(pid.equals("04"))return a*100.0/255.0;
        return (double)a;
    }
    public static Double adapterVolts(String response){
        if(response==null)return null;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)([0-9]+(?:\\.[0-9]+)?)\\s*V").matcher(response);
        if(!m.find())return null;double v=Double.parseDouble(m.group(1));return v>0&&v<=40?v:null;
    }
}
