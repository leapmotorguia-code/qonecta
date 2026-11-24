package com.headqlink.link;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Lanzador del C10 para aplicaciones INSTALADAS o sus webs oficiales.
 * No emula Android Auto ni CarPlay. No sortea DRM, privilegios OEM ni
 * limitaciones impuestas por el sistema multimedia.
 */
final class C10AppsScreen implements CarScreen {
    private static final int BG = 0xFF091C27;
    private static final int CARD = 0xFF183443;
    private static final int WHITE = 0xFFE8F5F5;
    private static final int MUTED = 0xFFB4C7CE;
    private static final int MINT = 0xFF68DBBD;
    private static final int AMBER = 0xFFF4CA86;

    private static final class App {
        final String label, pkg, url;
        final boolean video;
        App(String label, String pkg, String url, boolean video) {
            this.label=label;this.pkg=pkg;this.url=url;this.video=video;
        }
    }

    private static final App[] APPS = {
        new App("Google Maps", "com.google.android.apps.maps", "https://maps.google.com/", false),
        new App("Spotify", "com.spotify.music", "https://open.spotify.com/", false),
        new App("YouTube Music", "com.google.android.apps.youtube.music", "https://music.youtube.com/", false),
        new App("Netflix", "com.netflix.mediaclient", "https://www.netflix.com/", true),
        new App("Max / HBO", "com.wbd.stream", "https://www.max.com/", true),
        new App("Prime Video", "com.amazon.avod.thirdpartyclient", "https://www.primevideo.com/", true),
        new App("Pluto TV", "tv.pluto.android", "https://pluto.tv/", true)
    };

    private Context context;
    private LinearLayout list;
    private String pendingVideo = "";
    private long pendingVideoUntilMs;

    @Override public View create(Host host) {
        context = host.context();
        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(26, 20, 26, 30);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("APLICACIONES · UNIT C10 LAB", 33, WHITE, true);
        root.addView(title);
        TextView intro = text("Lanzador Android de aplicaciones instaladas. Si no existen, ofrece la web oficial. " +
                "La reproducción DRM de Netflix, Max y Prime depende del sistema del coche, no de este lanzador.",
                21, MUTED, false);
        intro.setPadding(0, 10, 0, 18);
        root.addView(intro);
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        TextView carplay = text(
                "Apple CarPlay: esta compilación NO incluye receptor CarPlay. " +
                "El iPhone solo funcionará si el equipo tiene otro receptor compatible (por ejemplo, AutoKit). " +
                "Android Auto utiliza el receptor independiente de este proyecto.",
                21, AMBER, false);
        carplay.setBackground(round(0xFF3D3527));
        carplay.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin=18;
        root.addView(carplay, lp);

        TextView refresh = text("↻  REVISAR APLICACIONES", 23, MINT, true);
        refresh.setGravity(Gravity.CENTER);
        refresh.setPadding(15, 20, 15, 20);
        refresh.setOnClickListener(v -> drawApps());
        root.addView(refresh);
        drawApps();
        return scroll;
    }

    private void drawApps() {
        if(list == null || context==null) return;
        list.removeAllViews();
        for (App a : APPS) {
            final Intent installed = installedIntent(a.pkg);
            boolean visible = installed != null;
            LinearLayout entry = new LinearLayout(context);
            entry.setOrientation(LinearLayout.VERTICAL);
            entry.setPadding(19, 15, 19, 15);
            entry.setBackground(round(CARD));
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elp.bottomMargin=10;
            list.addView(entry, elp);

            TextView name = text(a.label + (a.video ? "  ·  solo estacionado" : ""), 26, WHITE, true);
            entry.addView(name);
            TextView state = text(visible ? "Instalada y lanzable" :
                    "No se encuentra una versión Android lanzable; se ofrece la web oficial",
                    19, visible ? MINT : MUTED, false);
            state.setPadding(0, 5, 0, 10);
            entry.addView(state);
            TextView open = text(visible ? "ABRIR APLICACIÓN  ›" : "ABRIR WEB OFICIAL  ›", 22, MINT, true);
            open.setPadding(0, 6, 0, 6);
            entry.addView(open);
            entry.setOnClickListener(v -> tryOpen(a));
        }
    }

    private Intent installedIntent(String pkg) {
        try {
            return context.getPackageManager().getLaunchIntentForPackage(pkg);
        } catch (Exception e) {
            return null;
        }
    }

    private void tryOpen(App app) {
        if (app.video) {
            CarCloud.Snapshot s = CarCloud.car();
            if(s != null && s.hasData() && !s.demo &&
                    s.ageMs(System.currentTimeMillis()) < 120_000 &&
                    s.status != null && !Double.isNaN(s.status.speedKmh) &&
                    s.status.speedKmh >= 5.0) {
                Toast.makeText(context,
                        "Vídeo bloqueado: el vehículo comunica movimiento",
                        Toast.LENGTH_LONG).show();
                return;
            }
            // La UI puede vivir dentro de un Display/Presentation de LeapOS:
            // un AlertDialog normal no siempre recibe un Window token válido.
            // Doble pulsación deliberada, sin ventanas externas y sin impedir
            // que la proyección de Android Auto continúe.
            long now = android.os.SystemClock.elapsedRealtime();
            if (!app.pkg.equals(pendingVideo) || now > pendingVideoUntilMs) {
                pendingVideo = app.pkg;
                pendingVideoUntilMs = now + 9_000L;
                Toast.makeText(context,
                        "Vídeo solo estacionado. Pulsa una segunda vez para confirmar",
                        Toast.LENGTH_LONG).show();
                return;
            }
            pendingVideo = "";
            launch(app);
        } else {
            launch(app);
        }
    }

    private void launch(App app) {
        Intent intent = installedIntent(app.pkg);
        try {
            if (intent == null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.url));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, "Este Android no dispone de aplicación para abrir el enlace",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(context, "LeapOS ha rechazado el lanzamiento: " +
                    ex.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        if(bold)t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private static GradientDrawable round(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(18f);
        d.setStroke(1,0xFF386271);
        return d;
    }

    @Override public void destroy() {
        list=null;
        context=null;
    }
}
