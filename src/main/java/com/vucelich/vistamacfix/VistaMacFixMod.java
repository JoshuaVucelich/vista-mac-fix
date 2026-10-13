package com.vucelich.vistamacfix;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;

@Mod(value = VistaMacFixMod.MOD_ID, dist = Dist.CLIENT)
public final class VistaMacFixMod {
    public static final String MOD_ID = "vista_mac_fix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VistaMacFixMod(IEventBus modBus, ModContainer container) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            modBus.addListener(VistaMacFixMod::onClientSetup);
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info(
                "Vista Mac Fix {} loaded. Live viewfinder skip active={} (macOS, or -D{}=true)",
                containerVersion(),
                AppleGl.isActive(),
                AppleGl.PROPERTY
        );
    }

    private static String containerVersion() {
        return "1.0.0";
    }
}
