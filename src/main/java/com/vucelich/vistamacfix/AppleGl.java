package com.vucelich.vistamacfix;

import java.util.Locale;

/**
 * The workaround runs on macOS, or when {@code -Dvista_mac_fix.force=true}
 * (or {@code VISTA_MAC_FIX_FORCE=true}) is set so the path can be tested elsewhere.
 * {@code false} forces it off, including on a Mac.
 */
public final class AppleGl {
    public static final String PROPERTY = "vista_mac_fix.force";
    public static final String ENV = "VISTA_MAC_FIX_FORCE";

    private static Boolean active;

    private AppleGl() {
    }

    public static boolean isActive() {
        Boolean cached = active;
        if (cached != null) {
            return cached;
        }
        boolean value = detect();
        active = value;
        return value;
    }

    static void resetForTests() {
        active = null;
    }

    private static boolean detect() {
        String forced = forcedFlag();
        if (forced.equals("true") || forced.equals("1") || forced.equals("yes")) {
            return true;
        }
        if (forced.equals("false") || forced.equals("0") || forced.equals("no")) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("mac") || os.contains("darwin");
    }

    private static String forcedFlag() {
        String property = System.getProperty(PROPERTY);
        if (property != null && !property.isBlank()) {
            return property.trim().toLowerCase(Locale.ROOT);
        }
        String env = System.getenv(ENV);
        if (env != null && !env.isBlank()) {
            return env.trim().toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
