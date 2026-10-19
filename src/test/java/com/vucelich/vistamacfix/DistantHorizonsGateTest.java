package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DistantHorizonsGateTest {
    private static final String FEED = "com.vucelich.vistamacfix.mixin.DistantHorizonsFeedMixin";
    private static final String RATE = "com.vucelich.vistamacfix.mixin.VistaFeedRateMixin";
    private static final String SODIUM = "com.vucelich.vistamacfix.mixin.SodiumCopyBufferMixin";

    @AfterEach
    void restoreLoader() {
        DistantHorizonsGate.modLoaded = DistantHorizonsGate::loaderHas;
    }

    @Test
    void feedMixinAppliesOnlyWhenBothModsArePresent() {
        assertFalse(DistantHorizonsGate.applyMixin(FEED, false, false));
        assertFalse(DistantHorizonsGate.applyMixin(FEED, true, false));
        assertFalse(DistantHorizonsGate.applyMixin(FEED, false, true));
        assertTrue(DistantHorizonsGate.applyMixin(FEED, true, true));
        assertTrue(DistantHorizonsGate.applyMixin("net/mehvahdjukaar/vista/mixin/DistantHorizonsFeedMixin", true, true));
    }

    @Test
    void rateMixinAppliesWhenVistaIsPresent() {
        assertFalse(DistantHorizonsGate.applyMixin(RATE, false, false));
        assertFalse(DistantHorizonsGate.applyMixin(RATE, false, true));
        assertTrue(DistantHorizonsGate.applyMixin(RATE, true, false));
        assertTrue(DistantHorizonsGate.applyMixin(RATE, true, true));
    }

    @Test
    void existingMixinsStayApplied() {
        assertTrue(DistantHorizonsGate.applyMixin(SODIUM, false, false));
        assertTrue(DistantHorizonsGate.applyMixin(null, false, false));
        assertTrue(DistantHorizonsGate.applyMixin("TvContraptionMixin", false, true));
    }

    @Test
    void pluginUsesTheGateAndTheRealLoaderHasNeitherMod() {
        Predicate<String> previous = DistantHorizonsGate.modLoaded;
        try {
            DistantHorizonsGate.modLoaded = id -> false;
            VistaMacFixMixinPlugin plugin = new VistaMacFixMixinPlugin();
            assertFalse(plugin.shouldApplyMixin(
                    "net.mehvahdjukaar.vista.integration.distant_horizons.DistantHorizonsCompat",
                    FEED));
            assertFalse(plugin.shouldApplyMixin(
                    "net.mehvahdjukaar.vista.client.AdaptiveUpdateScheduler",
                    RATE));
            assertTrue(plugin.shouldApplyMixin(
                    "net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice$ImmediateCommandList",
                    SODIUM));

            DistantHorizonsGate.modLoaded = Set.of(DistantHorizonsGate.VISTA)::contains;
            assertFalse(plugin.shouldApplyMixin("DistantHorizonsCompat", FEED));
            assertTrue(plugin.shouldApplyMixin("AdaptiveUpdateScheduler", RATE));

            DistantHorizonsGate.modLoaded = Set.of(
                    DistantHorizonsGate.VISTA,
                    DistantHorizonsGate.DISTANT_HORIZONS)::contains;
            assertTrue(plugin.shouldApplyMixin("DistantHorizonsCompat", FEED));
        } finally {
            DistantHorizonsGate.modLoaded = previous;
        }
        assertFalse(DistantHorizonsGate.loaderHas(DistantHorizonsGate.VISTA));
        assertFalse(DistantHorizonsGate.loaderHas(DistantHorizonsGate.DISTANT_HORIZONS));
    }
}
