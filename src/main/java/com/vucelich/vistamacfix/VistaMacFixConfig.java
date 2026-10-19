package com.vucelich.vistamacfix;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VistaMacFixConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DH_GUARD;
    public static final ModConfigSpec.DoubleValue TV_UPDATE_FPS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Vista television feed and Distant Horizons.");
        DH_GUARD = builder
                .comment("When true, a Vista television feed does not change Distant Horizons config and does not draw DH LODs. Set false to restore Vista's own Distant Horizons compat.")
                .define("dhGuard", true);
        TV_UPDATE_FPS = builder
                .comment("Optional cap on Vista live-feed updates, in frames per second, the same unit as Vista's update_fps. 0 leaves that setting alone. Values are clamped to Vista's range of 1 to 60, then divided by 20 to match the scheduler's per-tick rate. The scheduler still will not apply a rate above 1.")
                .defineInRange("tvUpdateFps", 0.0, 0.0, 60.0);
        SPEC = builder.build();
    }

    private VistaMacFixConfig() {
    }

    public static boolean dhGuard() {
        try {
            return DH_GUARD.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static double tvUpdateFps() {
        try {
            return TV_UPDATE_FPS.get();
        } catch (IllegalStateException notLoaded) {
            return 0.0;
        }
    }
}
