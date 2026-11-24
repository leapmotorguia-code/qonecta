package com.leapmotorguia.c10control;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Navegador WebView Android independiente, sin prometer DRM ni acceso a apps OEM. */
public final class BrowserActivity extends Activity {
    private WebView web;
    private EditText address;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.BG);
        LinearLayout bar=Ui.row(this);bar.setPadding(Ui.dp(this,8),Ui.dp(this,7),Ui.dp(this,8),Ui.dp(this,7));
        android.widget.Button back=Ui.button(this,"‹",false);bar.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,50),Ui.dp(this,48)));
        address=Ui.number(this,"");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|
                android.text.InputType.TYPE_TEXT_VARIATION_URI);address.setSingleLine(true);
        address.setTextColor(Ui.TEXT);address.setHint("https://...");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f);lp.setMargins(Ui.dp(this,8),0,Ui.dp(this,8),0);bar.addView(address,lp);
        android.widget.Button go=Ui.button(this,"Ir",true);bar.addView(go,new LinearLayout.LayoutParams(Ui.dp(this,70),Ui.dp(this,48)));
        root.addView(bar);
        web=new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){address.setText(url);}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r){
                String scheme=r.getUrl().getScheme();return scheme==null||(!scheme.equals("http")&&!scheme.equals("https"));
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
        back.setOnClickListener(v->{if(web.canGoBack())web.goBack();else finish();});
        go.setOnClickListener(v->navigate());
        address.setOnEditorActionListener((v,id,event)->{
            if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_DONE){navigate();return true;}return false;});
        String start=getIntent().getStringExtra("url");
        web.loadUrl(start!=null&&start.startsWith("https://")?start:"https://www.google.com/");
    }
    private void navigate(){String s=address.getText().toString().trim();
        if(!s.startsWith("https://")&&!s.startsWith("http://"))s="https://"+s;
        web.loadUrl(s);
    }
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){
        if(web!=null){web.stopLoading();web.loadUrl("about:blank");web.destroy();web=null;}
        super.onDestroy();
    }
}
