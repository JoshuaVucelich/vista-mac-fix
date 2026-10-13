package com.vucelich.vistamacfix.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Marks the Vista 1.21.1-4.4.11 live-feed method. The plugin inserts the skip.
 */
@Mixin(targets = "net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer", remap = false)
public class VistaLiveFeedMixin44 {
}
