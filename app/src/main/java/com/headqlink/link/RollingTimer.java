package com.headqlink.link;

import java.util.Locale;

/** Recuperaciones estimadas con GPS, tras mantener 80 km/h durante tres segundos. */
final class RollingTimer {
    private static final double MAX_GAP_MS = 2500;
    private double previousMs = Double.NaN, previousSpeed, stableSince = Double.NaN;
    private double crossing80 = Double.NaN, startMs = Double.NaN;
    private boolean armed;
    private int next;
    private String result = "";
    private static final int[] TARGETS = {120, 130, 140};

    String fix(double ms, double speed) {
        if (Double.isNaN(ms) || Double.isInfinite(ms) || Double.isNaN(speed) || Double.isInfinite(speed) || speed < 0) { reset(); return null; }
        if (!Double.isNaN(previousMs) && (ms <= previousMs || ms - previousMs > MAX_GAP_MS)) reset();
        String output = null;
        if (!Double.isNaN(startMs)) {
            if (speed < 78 || ms - startMs > 60000) {
                reset();
            } else {
                while (next < TARGETS.length && speed >= TARGETS[next]) {
                    int target = TARGETS[next++];
                    double end = LaunchTimer.cross(previousMs, previousSpeed, ms, speed, target);
                    if (!result.isEmpty()) result += " · ";
                    result += String.format(Locale.getDefault(), "80-%d km/h: %.1f s", target, (end - startMs) / 1000);
                    output = result;
                }
                if (next == TARGETS.length) { reset(); }
            }
        } else if (speed >= 78 && speed <= 82) {
            if (Double.isNaN(stableSince)) stableSince = ms;
            if (!Double.isNaN(previousMs) && previousSpeed <= 80 && speed > 80)
                crossing80 = LaunchTimer.cross(previousMs, previousSpeed, ms, speed, 80);
            if (speed <= 80) crossing80 = Double.NaN;
            if (ms - stableSince >= 3000) armed = true;
        } else if (speed > 82 && armed && !Double.isNaN(previousMs)) {
            if (previousSpeed <= 80) crossing80 = LaunchTimer.cross(previousMs, previousSpeed, ms, speed, 80);
            if (!Double.isNaN(crossing80)) {
                startMs = crossing80;
                next = 0;
                result = "";
                while (next < TARGETS.length && speed >= TARGETS[next]) {
                    int target = TARGETS[next++];
                    double end = LaunchTimer.cross(previousMs, previousSpeed, ms, speed, target);
                    if (!result.isEmpty()) result += " · ";
                    result += String.format(Locale.getDefault(), "80-%d km/h: %.1f s", target, (end - startMs) / 1000);
                    output = result;
                }
                if (next == TARGETS.length) reset();
            } else reset();
        } else {
            reset();
        }
        previousMs = ms;
        previousSpeed = speed;
        return output;
    }

    void reset() {
        previousMs = stableSince = crossing80 = startMs = Double.NaN;
        armed = false;
        next = 0;
        result = "";
    }
}
