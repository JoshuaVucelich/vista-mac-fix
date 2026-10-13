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
    void takeoverFailureOnALitScreenCancelsTheOriginalRenderer() {
        assertTrue(ContraptionTvPolicy.cancelOriginalRenderer(false, true, true));
        assertTrue(ContraptionTvPolicy.cancelOriginalRenderer(true, true, true));
        assertTrue(ContraptionTvPolicy.cancelOriginalRenderer(true, false, true));
    }

    @Test
    void failedDrawFallsThroughOnlyWhenTakeoverIsOff() {
        assertFalse(ContraptionTvPolicy.cancelOriginalRenderer(false, false, true));
        assertFalse(ContraptionTvPolicy.cancelOriginalRenderer(false, true, false));
        assertFalse(ContraptionTvPolicy.cancelOriginalRenderer(false, false, false));
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
    void otherRenderFailuresStayEnabled() {
        assertFalse(ContraptionTvPolicy.onRenderFailure(new IllegalStateException("draw")));
        assertFalse(ContraptionTvPolicy.disabled());
        assertEquals(0, ContraptionTvPolicy.linkageLogs());
    }
}
