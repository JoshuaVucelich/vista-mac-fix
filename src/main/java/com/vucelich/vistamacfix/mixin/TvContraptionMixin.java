package com.vucelich.vistamacfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vucelich.vistamacfix.ContraptionTvPolicy;
import com.vucelich.vistamacfix.ContraptionTvScreen;
import com.vucelich.vistamacfix.SableContraptions;
import com.vucelich.vistamacfix.VistaFixNeutralizer;

import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * On a Sable sub-level the television's plot position is far from the camera,
 * and Vista 4.4.11 culls the screen. This draws it with the 4.4.11 video API.
 * {@code require = 0} so a different Vista method does not crash.
 * <p>
 * Vistafix overwrites this method. Mixin merges overwrites in the main pass and
 * applies injectors afterwards, so this head inject still runs and can cancel
 * the missing {@code getVideoFrameBuilder} call. Priority stays above vistafix
 * so a later injector from that mod cannot land in front of the cancel.
 */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.vista.client.renderer.TvBlockEntityRenderer", remap = false, priority = 2000)
public class TvContraptionMixin {
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED_MIXINS = new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.concurrent.atomic.AtomicBoolean LOGGED_FALLBACK = new java.util.concurrent.atomic.AtomicBoolean();
    static {
        org.slf4j.LoggerFactory.getLogger("vista_mac_fix")
                .info("Vista Mac Fix: television contraption renderer is armed");
    }

    @Inject(
            method = "render(Lnet/mehvahdjukaar/vista/common/tv/TVBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void vistaMacFix$renderOnContraption(
            TVBlockEntity television,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            CallbackInfo callback
    ) {
        boolean ship = SableContraptions.contains(television);
        boolean takeover = VistaFixNeutralizer.replaceEveryTelevision();
        if (!LOGGED_MIXINS.getAndSet(true)) {
            org.slf4j.LoggerFactory.getLogger("vista_mac_fix").info(
                    "Vista Mac Fix: television renderer ship={} vistafixTakeover={}",
                    ship,
                    takeover
            );
        }
        if (!ship && !takeover) {
            return;
        }
        boolean screenOn = television.isScreenOn(partialTick);
        boolean drew = ContraptionTvScreen.render(television, partialTick, pose, buffers, packedLight);
        if (ContraptionTvPolicy.cancelOriginalRenderer(drew, takeover, screenOn)) {
            if (!drew && LOGGED_FALLBACK.compareAndSet(false, true)) {
                org.slf4j.LoggerFactory.getLogger("vista_mac_fix").warn(
                        "Vista Mac Fix: television takeover did not draw; cancelling the original renderer so vistafix's missing call does not run"
                );
            }
            callback.cancel();
            return;
        }
        if (screenOn && LOGGED_FALLBACK.compareAndSet(false, true)) {
            org.slf4j.LoggerFactory.getLogger("vista_mac_fix").warn(
                    "Vista Mac Fix: contraption television did not draw; leaving Vista's renderer to draw this screen"
            );
        }
    }

    @ModifyReturnValue(method = "shouldRenderOffScreen(Lnet/mehvahdjukaar/vista/common/tv/TVBlockEntity;)Z", at = @At("RETURN"), require = 0)
    private boolean vistaMacFix$keepShipScreens(boolean original, TVBlockEntity television) {
        return original || SableContraptions.contains(television);
    }
}
