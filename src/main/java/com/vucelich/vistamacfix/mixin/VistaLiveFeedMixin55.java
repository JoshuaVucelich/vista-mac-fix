package com.vucelich.vistamacfix.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Marks the Vista 5.x live-feed method. Applied only when that method exists.
 */
@Mixin(targets = "net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer", remap = false)
public class VistaLiveFeedMixin55 {
}
