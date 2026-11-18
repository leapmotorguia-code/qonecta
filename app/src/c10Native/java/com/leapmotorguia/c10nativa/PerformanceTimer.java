package com.leapmotorguia.c10nativa;

import java.util.LinkedHashMap;
import java.util.Map;

/** GPS speed estimates only; the caller rejects stale or low-quality fixes. */
public final class PerformanceTimer {
    public enum Mode { FROM_STOP, RECOVERY }
    public enum State { WAITING, ARMED, TIMING, COMPLETE }
    private static final long STABLE_MS = 3000;
    private static final long MAX_GAP_MS = 3000;
    private static final int[] STOP_TARGETS = {50, 100, 120, 130, 140};
    private static final int[] RECOVERY_TARGETS = {100, 120, 130, 140};
    private final Mode mode;
    private final Map<Integer, Long> elapsed = new LinkedHashMap<>();
    private State state = State.WAITING;
    private long stableSince = -1, previousTime = -1, candidate80 = -1, startedAt = -1;
    private double previousSpeed;

    public PerformanceTimer(Mode mode) { this.mode = mode; }
    public State getState() { return state; }
    public Long elapsedAt(int target) { return elapsed.get(target); }
    public boolean awaiting80Crossing() { return mode == Mode.RECOVERY && state == State.ARMED && candidate80 < 0; }

    public void reset() {
        state = State.WAITING; stableSince = -1; previousTime = -1;
        candidate80 = -1; startedAt = -1; elapsed.clear();
    }

    private static long crossing(long aTime, double a, long bTime, double b, double threshold) {
        if (b <= a || bTime <= aTime) return bTime;
        double f = Math.max(0, Math.min(1, (threshold - a) / (b - a)));
        return aTime + Math.round((bTime - aTime) * f);
    }

    /** Returns false on invalid or interrupted input, in which case the attempt is reset. */
    public boolean accept(long timeMs, double speedKmh) {
        if (Double.isNaN(speedKmh) || Double.isInfinite(speedKmh) || speedKmh < 0 || speedKmh > 300 || timeMs < 0) { reset(); return false; }
        if (previousTime >= 0 && (timeMs <= previousTime || timeMs - previousTime > MAX_GAP_MS)) {
            reset();
            previousTime = timeMs; previousSpeed = speedKmh;
            return false;
        }
        if (state == State.COMPLETE) return true;
        boolean inRange = mode == Mode.FROM_STOP ? speedKmh <= 3 : speedKmh >= 78 && speedKmh <= 82;
        if (state == State.WAITING) {
            if (!inRange) stableSince = -1;
            else if (stableSince < 0) stableSince = timeMs;
            else if (timeMs - stableSince >= STABLE_MS) { state = State.ARMED; candidate80 = -1; }
        } else if (state == State.ARMED && previousTime >= 0) {
            if (mode == Mode.FROM_STOP && speedKmh > 3 && previousSpeed <= 3) {
                startedAt = crossing(previousTime, previousSpeed, timeMs, speedKmh, 3);
                state = State.TIMING;
            } else if (mode == Mode.RECOVERY) {
                if (candidate80 >= 0 && speedKmh < 80) candidate80 = -1;
                if (candidate80 < 0 && previousSpeed <= 80 && speedKmh > 80)
                    candidate80 = crossing(previousTime, previousSpeed, timeMs, speedKmh, 80);
                if (candidate80 >= 0 && speedKmh >= 82) {
                    startedAt = candidate80; state = State.TIMING;
                } else if (speedKmh < 78 || (candidate80 < 0 && speedKmh > 82)) {
                    state = State.WAITING; stableSince = -1; candidate80 = -1;
                } else if (candidate80 >= 0 && timeMs - candidate80 > 5000) candidate80 = -1;
            }
        }
        if (state == State.TIMING && previousTime >= 0) {
            int[] targets = mode == Mode.FROM_STOP ? STOP_TARGETS : RECOVERY_TARGETS;
            for (int target : targets) {
                if (!elapsed.containsKey(target) && previousSpeed < target && speedKmh >= target) {
                    long hit = crossing(previousTime, previousSpeed, timeMs, speedKmh, target);
                    if (hit >= startedAt) elapsed.put(target, hit - startedAt);
                }
            }
            if (elapsed.containsKey(140)) state = State.COMPLETE;
        }
        previousTime = timeMs; previousSpeed = speedKmh;
        return true;
    }
}
