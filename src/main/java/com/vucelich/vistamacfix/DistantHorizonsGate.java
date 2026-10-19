package com.vucelich.vistamacfix;

import java.util.function.Predicate;

/**
 * Decides which optional mixins are applied. The feed guard needs both Vista
 * and Distant Horizons. The feed-rate cap needs Vista only.
 */
public final class DistantHorizonsGate {
    public static final String FEED_MIXIN = "DistantHorizonsFeedMixin";
    public static final String RATE_MIXIN = "VistaFeedRateMixin";
    public static final String VISTA = "vista";
    public static final String DISTANT_HORIZONS = "distanthorizons";

    static volatile Predicate<String> modLoaded = DistantHorizonsGate::loaderHas;

    private DistantHorizonsGate() {
    }

    public static boolean isFeedMixin(String mixinClassName) {
        return endsWithClass(mixinClassName, FEED_MIXIN);
    }

    public static boolean isRateMixin(String mixinClassName) {
        return endsWithClass(mixinClassName, RATE_MIXIN);
    }

    public static boolean applyMixin(String mixinClassName, boolean vistaLoaded, boolean distantHorizonsLoaded) {
        if (isFeedMixin(mixinClassName)) {
            return vistaLoaded && distantHorizonsLoaded;
        }
        if (isRateMixin(mixinClassName)) {
            return vistaLoaded;
        }
        return true;
    }

    public static boolean applyMixin(String mixinClassName) {
        return applyMixin(mixinClassName, modLoaded.test(VISTA), modLoaded.test(DISTANT_HORIZONS));
    }

    static boolean loaderHas(String modId) {
        try {
            Class<?> type = Class.forName("net.neoforged.fml.loading.LoadingModList");
            Object list = type.getMethod("get").invoke(null);
            if (list == null) {
                return false;
            }
            Object file = list.getClass().getMethod("getModFileById", String.class).invoke(list, modId);
            return file != null;
        } catch (Throwable missing) {
            return false;
        }
    }

    private static boolean endsWithClass(String mixinClassName, String simpleName) {
        if (mixinClassName == null || simpleName == null) {
            return false;
        }
        return mixinClassName.equals(simpleName)
                || mixinClassName.endsWith("." + simpleName)
                || mixinClassName.endsWith("/" + simpleName);
    }
}
