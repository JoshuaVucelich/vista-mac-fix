package com.vucelich.vistamacfix;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Started only when {@code -Dvista_mac_fix.dhProbe=true}. Calls Vista's feed
 * wrapper a few times and records whether that created a Distant Horizons
 * delayed-config timer.
 */
public final class DhFeedProbe {
    private DhFeedProbe() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("vista_mac_fix.dhProbe");
    }

    public static void run() {
        int before = delayedTimers();
        int runs = 0;
        String wrapper = "missing";
        Throwable error = null;
        try {
            Class<?> type = Class.forName(
                    "net.mehvahdjukaar.vista.integration.distant_horizons.DistantHorizonsCompat");
            Method method = type.getMethod("decorateRenderWithoutLOD", Runnable.class);
            for (int i = 0; i < 5; i++) {
                Object wrapped = method.invoke(null, (Runnable) () -> {
                });
                wrapper = wrapped == null ? "null" : wrapped.getClass().getName();
                if (wrapped instanceof Runnable runnable) {
                    runnable.run();
                    runs++;
                }
            }
        } catch (Throwable thrown) {
            error = thrown;
        }
        int after = delayedTimers();
        List<String> names = delayedTimerNames();
        boolean guardWrapper = wrapper.contains("VistaFeedGuard");
        boolean pass = error == null && runs == 5 && guardWrapper && after == before && VistaFeedGuard.depth == 0;
        String report = "pass=" + pass
                + " runs=" + runs
                + " wrapper=" + wrapper
                + " timersBefore=" + before
                + " timersAfter=" + after
                + " timerNames=" + names
                + " depth=" + VistaFeedGuard.depth
                + " error=" + (error == null ? "none" : error.toString());
        VistaMacFixMod.LOGGER.info("Vista Mac Fix DH probe: {}", report);
        String path = System.getProperty("vista_mac_fix.dhProbeFile", "run/dh-probe.txt");
        try {
            Path file = Path.of(path);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, report + System.lineSeparator());
        } catch (Throwable thrown) {
            VistaMacFixMod.LOGGER.warn("Vista Mac Fix DH probe: could not write {}", path, thrown);
        }
        if (Boolean.getBoolean("vista_mac_fix.dhProbeExit")) {
            System.exit(pass ? 0 : 1);
        }
    }

    static int delayedTimers() {
        return delayedTimerNames().size();
    }

    private static List<String> delayedTimerNames() {
        List<String> names = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String name = thread.getName();
            if (name != null && name.contains("AbstractDelayedConfigTimer")) {
                names.add(name);
            }
        }
        return names;
    }
}
