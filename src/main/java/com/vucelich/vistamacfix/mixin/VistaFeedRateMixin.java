package com.vucelich.vistamacfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.vucelich.vistamacfix.VistaFeedRate;
import com.vucelich.vistamacfix.VistaMacFixConfig;

/**
 * Caps Vista's live-feed scheduler when {@code tvUpdateFps} is set.
 * {@code require = 0} so a renamed method does not crash. A cap of 0
 * returns the original rate.
 */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.vista.client.AdaptiveUpdateScheduler", remap = false)
public class VistaFeedRateMixin {
    @ModifyReturnValue(method = "computeEffectiveUpdateRate", at = @At("RETURN"), require = 0, remap = false)
    private double vistaMacFix$capFeedRate(double ratePerTick) {
        return VistaFeedRate.capPerTick(ratePerTick, VistaMacFixConfig.tvUpdateFps());
    }
}
