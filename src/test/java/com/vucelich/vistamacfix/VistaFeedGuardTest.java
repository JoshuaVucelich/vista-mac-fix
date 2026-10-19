package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VistaFeedGuardTest {
    @BeforeEach
    @AfterEach
    void resetDepth() {
        VistaFeedGuard.depth = 0;
    }

    @Test
    void wrapRunsOnceAndReturnsDepthToZero() {
        AtomicInteger runs = new AtomicInteger();
        Runnable wrapped = VistaFeedGuard.wrap(() -> {
            assertEquals(1, VistaFeedGuard.depth);
            assertTrue(VistaFeedGuard.hidesLods());
            runs.incrementAndGet();
        });
        wrapped.run();
        assertEquals(1, runs.get());
        assertEquals(0, VistaFeedGuard.depth);
        assertFalse(VistaFeedGuard.hidesLods());
    }

    @Test
    void exceptionInsideTaskStillDecrementsDepth() {
        Runnable wrapped = VistaFeedGuard.wrap(() -> {
            assertEquals(1, VistaFeedGuard.depth);
            throw new IllegalStateException("feed failed");
        });
        IllegalStateException error = assertThrows(IllegalStateException.class, wrapped::run);
        assertEquals("feed failed", error.getMessage());
        assertEquals(0, VistaFeedGuard.depth);
        assertFalse(VistaFeedGuard.hidesLods());
    }

    @Test
    void nestedFeedsUnstack() {
        assertThrows(IllegalStateException.class, () -> VistaFeedGuard.wrap(() -> {
            VistaFeedGuard.wrap(() -> {
                assertEquals(2, VistaFeedGuard.depth);
                throw new IllegalStateException("inner");
            }).run();
        }).run());
        assertEquals(0, VistaFeedGuard.depth);
    }

    @Test
    void disabledGuardLeavesTheOriginalMethodInPlace() {
        assertNull(VistaFeedGuard.replacing(() -> {
        }, false));
        assertNull(VistaFeedGuard.replacing(null, true));
        AtomicInteger runs = new AtomicInteger();
        Runnable wrapped = VistaFeedGuard.replacing(runs::incrementAndGet, true);
        wrapped.run();
        assertEquals(1, runs.get());
        assertEquals(0, VistaFeedGuard.depth);
    }
}
