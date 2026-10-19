package com.vucelich.vistamacfix;

/**
 * Counts nested Vista television feed renders so Distant Horizons can skip
 * LOD drawing for that pass. The feed mixin is the only writer.
 */
public final class VistaFeedGuard {
    public static int depth;

    private VistaFeedGuard() {
    }

    public static boolean hidesLods() {
        return depth > 0;
    }

    /**
     * @return a single run of {@code task} while {@link #depth} is positive,
     *         or null when the guard is off so Vista's original wrapper stays
     */
    public static Runnable replacing(Runnable task, boolean enabled) {
        if (!enabled || task == null) {
            return null;
        }
        return wrap(task);
    }

    public static Runnable wrap(Runnable task) {
        return () -> {
            depth++;
            try {
                task.run();
            } finally {
                depth--;
            }
        };
    }
}
