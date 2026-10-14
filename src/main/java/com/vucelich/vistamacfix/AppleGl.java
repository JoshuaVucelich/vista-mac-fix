package com.vucelich.vistamacfix;

import java.util.Locale;

/**
 * The CPU copy runs when {@code os.name} contains {@code Mac}, which is the
 * Apple OpenGL client. {@code -Dvista_mac_fix.force=true} (or the
 * {@code VISTA_MAC_FIX_FORCE} environment variable) turns the same path on
 * elsewhere so it can be tested. {@code false} forces it off, including on a Mac.
 */
public final class AppleGl {
    public static final String PROPERTY = "vista_mac_fix.force";
    public static final String ENV = "VISTA_MAC_FIX_FORCE";
    public static final String TRAP_PROPERTY = "vista_mac_fix.trap";

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

    public static boolean trapNativeCalls() {
        return Boolean.getBoolean(TRAP_PROPERTY);
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
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("mac");
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
