package com.vucelich.vistamacfix;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decisions for the contraption television draw that do not touch Minecraft.
 * Tests cover these without loading Vista.
 */
public final class ContraptionTvPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean DISABLED = new AtomicBoolean();
    private static final AtomicInteger LINKAGE_LOGS = new AtomicInteger();

    private ContraptionTvPolicy() {
    }

    static boolean disabled() {
        return DISABLED.get();
    }

    static int linkageLogs() {
        return LINKAGE_LOGS.get();
    }

    static void resetForTests() {
        DISABLED.set(false);
        LINKAGE_LOGS.set(0);
    }

    /**
     * A successful draw always replaces the original renderer. A failed draw
     * cancels it only while vistafix's overwrite is still in the method and the
     * screen is lit: falling through would hit that mod's missing 3-int call.
     * With takeover off, the original Vista renderer still runs.
     */
    public static boolean cancelOriginalRenderer(boolean drew, boolean takeover, boolean screenOn) {
        if (drew) {
            return true;
        }
        return takeover && screenOn;
    }

    /**
     * A {@link LinkageError} (Vista 5.x without the 4.4.11 method, for example)
     * disables this draw for the rest of the session. The warning is written once.
     *
     * @return true when the failure disabled the path
     */
    static boolean onRenderFailure(Throwable error) {
        if (!(error instanceof LinkageError)) {
            LOGGER.warn("Vista Mac Fix: contraption television render failed", error);
            return false;
        }
        if (DISABLED.compareAndSet(false, true)) {
            LINKAGE_LOGS.incrementAndGet();
            LOGGER.warn(
                    "Vista Mac Fix: contraption television render is disabled after a linkage error ({})",
                    error.getClass().getName(),
                    error
            );
        }
        return true;
    }
}
