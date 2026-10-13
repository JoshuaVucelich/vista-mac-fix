package com.vucelich.vistamacfix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;

@Mod(value = VistaMacFixMod.MOD_ID, dist = Dist.CLIENT)
public final class VistaMacFixMod {
    public static final String MOD_ID = "vista_mac_fix";
    public static final String VERSION = "1.0.1";
    public static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");

    public VistaMacFixMod(IEventBus modBus, ModContainer container) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            modBus.addListener(VistaMacFixMod::onClientSetup);
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info(
                "Vista Mac Fix {} loaded. Apple buffer-copy fallback active={} (os.name contains Mac, or -D{}=true)",
                VERSION,
                AppleGl.isActive(),
                AppleGl.PROPERTY
        );
    }
}
