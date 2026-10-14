package com.vucelich.vistamacfix;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decisions for the contraption television draw that do not touch Minecraft.
 * Tests cover these without loading Vista.
 */
public final class ContraptionTvPolicy {
    static final long NON_LINKAGE_STACK_INTERVAL_NANOS = 5_000_000_000L;

    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean DISABLED = new AtomicBoolean();
    private static final AtomicInteger LINKAGE_LOGS = new AtomicInteger();
    private static final AtomicInteger NON_LINKAGE_STACKS = new AtomicInteger();
    private static final AtomicLong LAST_NON_LINKAGE_STACK = new AtomicLong(Long.MIN_VALUE);

    private ContraptionTvPolicy() {
    }

    static boolean disabled() {
        return DISABLED.get();
    }

    static int linkageLogs() {
        return LINKAGE_LOGS.get();
    }

    static int nonLinkageStacks() {
        return NON_LINKAGE_STACKS.get();
    }

    static void resetForTests() {
        DISABLED.set(false);
        LINKAGE_LOGS.set(0);
        NON_LINKAGE_STACKS.set(0);
        LAST_NON_LINKAGE_STACK.set(Long.MIN_VALUE);
    }

    /**
     * A successful draw replaces the original renderer. While vistafix's
     * overwrite is still in the method, the original is cancelled for every
     * television, lit or not, so that missing 3-int call never runs. With
     * takeover off, Vista's renderer still runs.
     */
    public static boolean cancelOriginalRenderer(boolean drew, boolean takeover) {
        return drew || takeover;
    }

    /**
     * A {@link LinkageError} (Vista 5.x without the 4.4.11 method, for example)
     * disables this draw for the rest of the session. The warning is written once.
     * Other failures keep the path enabled and log a stack at most once every
     * five seconds.
     *
     * @return true when the failure disabled the path
     */
    public static boolean onRenderFailure(Throwable error) {
        return onRenderFailure(error, System.nanoTime());
    }

    static boolean onRenderFailure(Throwable error, long nowNanos) {
        if (!(error instanceof LinkageError)) {
            logNonLinkage(error, nowNanos);
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

    private static void logNonLinkage(Throwable error, long nowNanos) {
        long previous = LAST_NON_LINKAGE_STACK.get();
        if (previous != Long.MIN_VALUE && nowNanos - previous < NON_LINKAGE_STACK_INTERVAL_NANOS) {
            return;
        }
        if (!LAST_NON_LINKAGE_STACK.compareAndSet(previous, nowNanos)) {
            return;
        }
        NON_LINKAGE_STACKS.incrementAndGet();
        LOGGER.warn("Vista Mac Fix: contraption television render failed", error);
    }
}
