package com.vucelich.vistamacfix;

/**
 * Vista 4.4.11 stores live-feed updates as a rate per tick. {@code baseFps}
 * divides the configured frames per second by 20 before the scheduler sees it.
 */
public final class VistaFeedRate {
    public static final double TICKS_PER_SECOND = 20.0;

    private VistaFeedRate() {
    }

    public static double capPerTick(double ratePerTick, double capFps) {
        if (!(capFps > 0.0) || Double.isNaN(capFps) || Double.isNaN(ratePerTick)) {
            return ratePerTick;
        }
        return Math.min(ratePerTick, capFps / TICKS_PER_SECOND);
    }
}
