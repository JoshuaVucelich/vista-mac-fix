package com.vucelich.vistamacfix;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDeferredRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;

/**
 * Dev-run only. Not packaged in the release jar. Calls Vista's feed wrapper
 * and fires the DH render events the guard binds.
 */
public final class DhFeedProbe {
    private DhFeedProbe() {
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
        String events = fireEvents();
        boolean guardWrapper = wrapper.contains("VistaFeedGuard");
        boolean eventsOk = events.contains("beforeOutside=false")
                && events.contains("deferredOutside=false")
                && events.contains("beforeInside=true")
                && events.contains("deferredInside=true");
        boolean pass = error == null
                && runs == 5
                && guardWrapper
                && after == before
                && VistaFeedGuard.depth() == 0
                && eventsOk;
        String report = "pass=" + pass
                + " runs=" + runs
                + " wrapper=" + wrapper
                + " timersBefore=" + before
                + " timersAfter=" + after
                + " timerNames=" + names
                + " depth=" + VistaFeedGuard.depth()
                + " " + events
                + " sodium=" + DistantHorizonsGate.loaderHas("sodium")
                + " error=" + (error == null ? "none" : error.toString());
        VistaMacFixMod.LOGGER.info("Vista Mac Fix DH probe: {}", report);
        String path = System.getProperty("vista_mac_fix.dhProbeFile", "dh-probe.txt");
        try {
            Path file = Path.of(path).toAbsolutePath();
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, report + System.lineSeparator());
            VistaMacFixMod.LOGGER.info("Vista Mac Fix DH probe file: {}", file);
        } catch (Throwable thrown) {
            VistaMacFixMod.LOGGER.warn("Vista Mac Fix DH probe: could not write {}", path, thrown);
        }
        if (Boolean.getBoolean("vista_mac_fix.dhProbeExit")) {
            System.exit(pass ? 0 : 1);
        }
    }

    private static String fireEvents() {
        try {
            boolean beforeOutside = fire(DhApiBeforeRenderEvent.class);
            boolean deferredOutside = fire(DhApiBeforeDeferredRenderEvent.class);
            boolean[] inside = new boolean[2];
            VistaFeedGuard.wrap(() -> {
                inside[0] = fire(DhApiBeforeRenderEvent.class);
                inside[1] = fire(DhApiBeforeDeferredRenderEvent.class);
            }).run();
            return "beforeOutside=" + beforeOutside
                    + " deferredOutside=" + deferredOutside
                    + " beforeInside=" + inside[0]
                    + " deferredInside=" + inside[1];
        } catch (Throwable thrown) {
            return "events=error:" + thrown;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean fire(Class eventType) {
        return DhApi.events.fireAllEvents(eventType, null);
    }

    private static int delayedTimers() {
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
