package com.leapmotorguia.c10nativa;

import android.os.Build;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;

/** Applies the mandatory edge-to-edge safe area on Android 15 and later. */
final class ScreenInsets {
    static void apply(View root) {
        if (Build.VERSION.SDK_INT < 35) return;
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        root.requestApplyInsets();
    }
}
