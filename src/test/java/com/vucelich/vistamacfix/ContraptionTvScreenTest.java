package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ContraptionTvScreenTest {
    @AfterEach
    void reset() {
        ContraptionTvPolicy.resetForTests();
    }

    @Test
    void takeoverCancelsTheOriginalRendererEvenWhenTheScreenIsOff() {
        assertTrue(ContraptionTvPolicy.cancelOriginalRenderer(false, true));
        assertTrue(ContraptionTvPolicy.cancelOriginalRenderer(true, true));
        assertTrue(ContraptionTvPolicy.cancelOriginalRenderer(true, false));
    }

    @Test
    void failedDrawFallsThroughOnlyWhenTakeoverIsOff() {
        assertFalse(ContraptionTvPolicy.cancelOriginalRenderer(false, false));
    }

    @Test
    void linkageErrorDisablesTheDrawAfterOneLog() {
        assertFalse(ContraptionTvPolicy.disabled());
        assertTrue(ContraptionTvPolicy.onRenderFailure(new NoSuchMethodError("getVideoFrameBuilder")));
        assertTrue(ContraptionTvPolicy.disabled());
        assertEquals(1, ContraptionTvPolicy.linkageLogs());

        assertTrue(ContraptionTvPolicy.onRenderFailure(new NoSuchMethodError("getVideoFrameBuilder")));
        assertEquals(1, ContraptionTvPolicy.linkageLogs());
    }

    @Test
    void otherRenderFailuresStayEnabledAndRateLimitStacks() {
        long start = 1_000L;
        assertFalse(ContraptionTvPolicy.onRenderFailure(new IllegalStateException("draw"), start));
        assertEquals(1, ContraptionTvPolicy.nonLinkageStacks());
        assertFalse(ContraptionTvPolicy.onRenderFailure(new IllegalStateException("draw"), start + 1L));
        assertEquals(1, ContraptionTvPolicy.nonLinkageStacks());
        assertFalse(ContraptionTvPolicy.onRenderFailure(
                new IllegalStateException("draw"),
                start + ContraptionTvPolicy.NON_LINKAGE_STACK_INTERVAL_NANOS
        ));
        assertEquals(2, ContraptionTvPolicy.nonLinkageStacks());
        assertFalse(ContraptionTvPolicy.disabled());
        assertEquals(0, ContraptionTvPolicy.linkageLogs());
    }
}
