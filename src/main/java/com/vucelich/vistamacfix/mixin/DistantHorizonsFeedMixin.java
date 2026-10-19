package com.vucelich.vistamacfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.vucelich.vistamacfix.VistaFeedGuard;
import com.vucelich.vistamacfix.VistaMacFixConfig;

/**
 * Vista 4.4.11 wraps every television feed render with Distant Horizons
 * config writes. Those writes start a delayed LOD reload. This returns the
 * task once, with no config calls. {@code require = 0} so a renamed method
 * does not crash.
 */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.vista.integration.distant_horizons.DistantHorizonsCompat", remap = false)
public class DistantHorizonsFeedMixin {
    static {
        org.slf4j.LoggerFactory.getLogger("vista_mac_fix").info(
                "Vista Mac Fix: Distant Horizons feed guard mixin applied to DistantHorizonsCompat.decorateRenderWithoutLOD"
        );
    }

    @Inject(
            method = "decorateRenderWithoutLOD(Ljava/lang/Runnable;)Ljava/lang/Runnable;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void vistaMacFix$skipDhConfig(Runnable task, CallbackInfoReturnable<Runnable> callback) {
        Runnable guarded = VistaFeedGuard.replacing(task, VistaMacFixConfig.dhGuard());
        if (guarded != null) {
            callback.setReturnValue(guarded);
        }
    }
}
