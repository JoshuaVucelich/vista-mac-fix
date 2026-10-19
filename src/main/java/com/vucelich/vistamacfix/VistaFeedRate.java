package com.vucelich.vistamacfix;

/**
 * Vista 4.4.11 {@code update_fps} is frames per second, bounded to 1 through
 * 60. {@code AdaptiveUpdateScheduler.baseFps} divides that number by 20 and
 * stores updates per tick. {@code computeEffectiveUpdateRate} then clamps the
 * applied rate to at most 1.
 */
public final class VistaFeedRate {
    public static final double TICKS_PER_SECOND = 20.0;
    /** Same bounds as Vista 4.4.11 {@code update_fps}. */
    public static final double MIN_FPS = 1.0;
    public static final double MAX_FPS = 60.0;

    private VistaFeedRate() {
    }

    public static double capPerTick(double ratePerTick, double capFps) {
        if (!(capFps > 0.0) || Double.isNaN(capFps) || Double.isNaN(ratePerTick)) {
            return ratePerTick;
        }
        double fps = Math.min(MAX_FPS, Math.max(MIN_FPS, capFps));
        double perTick = Math.min(1.0, fps / TICKS_PER_SECOND);
        return Math.min(ratePerTick, perTick);
    }
}
