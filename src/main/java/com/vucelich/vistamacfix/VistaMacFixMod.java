package com.vucelich.vistamacfix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = VistaMacFixMod.MOD_ID, dist = Dist.CLIENT)
public final class VistaMacFixMod {
    public static final String MOD_ID = "vista_mac_fix";
    public static final String VERSION = "1.0.5";
    public static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");

    public VistaMacFixMod(IEventBus modBus, ModContainer container) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            // Mixin configs are already prepared. Drop vistafix's broken overwrite
            // before Vista's renderer class is loaded.
            VistaFixNeutralizer.neutralize(true);
            if (ShipTvTest.enabled()) {
                NeoForge.EVENT_BUS.addListener(ShipTvTest::onClientTick);
            }
            modBus.addListener(VistaMacFixMod::onClientSetup);
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info(
                "Vista Mac Fix {} loaded. Apple buffer-copy fallback active={} (os.name contains Mac, or -D{}=true). Contraption televisions use the Vista 4.4.11 renderer.",
                VERSION,
                AppleGl.isActive(),
                AppleGl.PROPERTY
        );
        // Loading the targets applies the mixins before the first copy, including
        // Sable's resize, which may not run until an airship is drawn.
        arm("net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice$ImmediateCommandList", "Sodium");
        arm("dev.ryanhcode.sable.sublevel.render.fancy.BucketRenderBuffer", "Sable");
    }

    private static void arm(String binaryName, String label) {
        try {
            Class.forName(binaryName, true, VistaMacFixMod.class.getClassLoader());
            LOGGER.info("Vista Mac Fix: {} copy redirect is armed ({})", label, binaryName);
        } catch (ClassNotFoundException missing) {
            LOGGER.info("Vista Mac Fix: {} is not installed; its copy redirect stays dormant", label);
        } catch (Throwable error) {
            LOGGER.warn("Vista Mac Fix: could not arm the {} copy redirect", label, error);
        }
    }
}
