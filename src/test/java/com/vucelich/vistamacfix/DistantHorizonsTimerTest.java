package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.junit.jupiter.api.Test;

/**
 * Distant Horizons 3.0.3 starts a thread named DH-AbstractDelayedConfigTimer
 * from ReloadLodsConfigEventHandler.onConfigValueSet. The feed guard's
 * wrapper does not.
 */
class DistantHorizonsTimerTest {
    @Test
    void configSetStartsTimerAndGuardDoesNot() throws Exception {
        assertTrue(jarPresent(), "vista.dh.jar was not set to the Distant Horizons 3.0.3 jar");
        Path jar = jar();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> type = loader.loadClass(
                    "com.seibel.distanthorizons.core.config.eventHandlers.ReloadLodsConfigEventHandler");
            Object delayed = type.getField("DELAYED_INSTANCE").get(null);
            int before = timerNames().size();
            type.getMethod("onConfigValueSet").invoke(delayed);
            List<String> started = timerNames();
            assertTrue(started.size() > before, started.toString());
            assertTrue(started.stream().anyMatch(name -> name.contains("AbstractDelayedConfigTimer")), started.toString());
            cancel(delayed);

            List<String> beforeGuard = List.copyOf(timerNames());
            Runnable wrapped = VistaFeedGuard.wrap(() -> assertEquals(1, VistaFeedGuard.depth()));
            wrapped.run();
            for (String name : timerNames()) {
                assertTrue(beforeGuard.contains(name), "feed guard started " + name);
            }
            assertEquals(0, VistaFeedGuard.depth());
        }
    }

    static boolean jarPresent() {
        String path = System.getProperty("vista.dh.jar", "");
        return !path.isEmpty() && Files.isRegularFile(Path.of(path));
    }

    private static Path jar() {
        return Path.of(System.getProperty("vista.dh.jar"));
    }

    private static void cancel(Object handler) throws Exception {
        Class<?> type = handler.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("timer");
                field.setAccessible(true);
                Object timer = field.get(handler);
                if (timer instanceof Timer live) {
                    live.cancel();
                }
                return;
            } catch (NoSuchFieldException missing) {
                type = type.getSuperclass();
            }
        }
    }

    private static List<String> timerNames() {
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
