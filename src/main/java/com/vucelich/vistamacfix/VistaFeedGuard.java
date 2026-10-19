package com.vucelich.vistamacfix;

/**
 * Counts nested Vista television feed renders on the thread that entered
 * them, so Distant Horizons can skip LOD drawing for that pass. The feed
 * mixin is the only writer.
 */
public final class VistaFeedGuard {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private VistaFeedGuard() {
    }

    public static int depth() {
        return DEPTH.get()[0];
    }

    public static boolean hidesLods() {
        return depth() > 0;
    }

    /**
     * @return a single run of {@code task} while {@link #depth()} is positive
     *         on this thread, or null when the guard is off so Vista's
     *         original wrapper stays
     */
    public static Runnable replacing(Runnable task, boolean enabled) {
        if (!enabled || task == null) {
            return null;
        }
        return wrap(task);
    }

    public static Runnable wrap(Runnable task) {
        return () -> {
            int[] depth = DEPTH.get();
            depth[0]++;
            try {
                task.run();
            } finally {
                depth[0]--;
            }
        };
    }

    static void reset() {
        DEPTH.remove();
    }
}
