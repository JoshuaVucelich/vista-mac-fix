package com.vucelich.vistamacfix;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.service.MixinService;

import net.neoforged.fml.loading.FMLLoader;

/**
 * Vista Aeronautics Fix 1.0.0 overwrites the television renderer with a
 * {@code getVideoFrameBuilder} call that Vista 4.4.11 does not have. Removing
 * that mixin before the renderer class loads keeps the original method, and
 * this mod's contraption path draws the screen.
 */
public final class VistaFixNeutralizer {
    static final String CONFIG_NAME = "vistafix.mixins.json";
    static final String MIXIN_CLASS = "net/mehvahdjukaar/vistafix/mixin/TvRendererMixin.class";
    /**
     * Three raw ints between the booleans. Vista 4.4.11 takes two {@code Vec2i}
     * values and a trailing boolean instead.
     */
    static final byte[] BROKEN_CALL = "ZIIIZ".getBytes(StandardCharsets.US_ASCII);
    static final byte[] FRAME_BUILDER = "getVideoFrameBuilder".getBytes(StandardCharsets.US_ASCII);

    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    private static volatile boolean settled;
    private static volatile boolean replaceEveryTelevision;

    private VistaFixNeutralizer() {
    }

    public static boolean replaceEveryTelevision() {
        return replaceEveryTelevision;
    }

    /**
     * Called after our television mixin is applied. If vistafix's overwrite is
     * already in the method, every lit screen is drawn here so that call never runs.
     */
    public static void noteRenderedClass(ClassNode targetClass) {
        if (classCallsMissingFrameBuilder(targetClass)) {
            replaceEveryTelevision = true;
            settled = true;
            logOnce("Vista Mac Fix: vistafix TvRendererMixin is in the television renderer. This mod draws the screen and skips that missing getVideoFrameBuilder call.");
            return;
        }
        if (replaceEveryTelevision) {
            replaceEveryTelevision = false;
            LOGGER.info("Vista Mac Fix: television renderer does not contain the missing getVideoFrameBuilder call. Only Sable contraptions use this mod's screen.");
        }
    }

    /**
     * @param finalAttempt when true, a missing vistafix jar is the answer and
     *                     later calls do nothing
     */
    public static void neutralize(boolean finalAttempt) {
        if (settled) {
            return;
        }
        try {
            byte[] bytes = readMixinBytes();
            boolean broken = bytesCallMissingFrameBuilder(bytes);
            List<String> names = new ArrayList<>();
            int removed = stripConfigs(names);
            boolean sawConfig = false;
            for (String name : names) {
                if (CONFIG_NAME.equals(name)) {
                    sawConfig = true;
                    break;
                }
            }
            boolean stillQueued = brokenMixinStillQueued();
            if (removed > 0 && !stillQueued) {
                settled = true;
                replaceEveryTelevision = false;
                logOnce("Vista Mac Fix: stopped vistafix TvRendererMixin. Its getVideoFrameBuilder call is not in Vista 4.4.11. This mod draws televisions on Sable contraptions.");
                return;
            }
            if (bytes != null && !broken) {
                settled = true;
                logOnce("Vista Mac Fix: vistafix TvRendererMixin does not call the missing method; leaving it in place");
                return;
            }
            if (!broken && !sawConfig) {
                if (finalAttempt) {
                    settled = true;
                    logOnce("Vista Mac Fix: vistafix is not installed; television contraptions use this mod");
                }
                return;
            }
            // Our plugin loads before later mixin configs are registered, and the
            // mod classloader does not see the transformer's config set. Keep
            // trying until the constructor, then fall back to drawing every screen.
            if (!finalAttempt && !sawConfig) {
                return;
            }
            if (sawConfig && !stillQueued) {
                settled = true;
                logOnce("Vista Mac Fix: vistafix TvRendererMixin is not queued. This mod draws televisions on Sable contraptions.");
                return;
            }
            if (finalAttempt) {
                logConfigNames(names);
            }
            if (!sawConfig && !finalAttempt) {
                return;
            }
            if (stillQueued || (broken && finalAttempt)) {
                replaceEveryTelevision = true;
                if (finalAttempt || stillQueued) {
                    settled = true;
                }
                LOGGER.info(
                        "Vista Mac Fix: vistafix TvRendererMixin is still queued (removed {}, configs {}). This mod will draw televisions so that missing call does not run.",
                        removed,
                        names
                );
            }
        } catch (Throwable error) {
            replaceEveryTelevision = true;
            if (finalAttempt) {
                settled = true;
            }
            LOGGER.warn("Vista Mac Fix: could not unhook vistafix TvRendererMixin", error);
        }
    }

    static boolean classCallsMissingFrameBuilder(ClassNode node) {
        if (node == null || node.methods == null) {
            return false;
        }
        for (MethodNode method : node.methods) {
            if (method == null || method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) {
                    continue;
                }
                if ("getVideoFrameBuilder".equals(call.name) && call.desc != null && call.desc.contains("ZIIIZ")) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean bytesCallMissingFrameBuilder(byte[] bytes) {
        return bytes != null && indexOf(bytes, FRAME_BUILDER) >= 0 && indexOf(bytes, BROKEN_CALL) >= 0;
    }

    /**
     * Drops matching entries from the live mixin tables. Returns how many
     * mixin infos were removed.
     */
    static int removeMatching(
            Map<String, List<Object>> mapping,
            List<Object> mixins,
            List<Object> pending,
            List<String> classNames,
            java.util.function.Predicate<Object> dropInfo,
            java.util.function.Predicate<String> dropName
    ) {
        int removed = 0;
        if (mapping != null) {
            for (List<Object> list : mapping.values()) {
                if (list == null) {
                    continue;
                }
                for (Iterator<Object> it = list.iterator(); it.hasNext(); ) {
                    if (dropInfo.test(it.next())) {
                        it.remove();
                        removed++;
                    }
                }
            }
        }
        removed += dropFrom(mixins, dropInfo);
        removed += dropFrom(pending, dropInfo);
        if (classNames != null && dropName != null) {
            classNames.removeIf(dropName::test);
        }
        return removed;
    }

    private static int dropFrom(List<Object> list, java.util.function.Predicate<Object> dropInfo) {
        if (list == null) {
            return 0;
        }
        int removed = 0;
        for (Iterator<Object> it = list.iterator(); it.hasNext(); ) {
            if (dropInfo.test(it.next())) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    private static byte[] readMixinBytes() {
        try (InputStream in = MixinService.getService().getResourceAsStream(MIXIN_CLASS)) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (Throwable ignored) {
            // The service may not see the jar yet. The classloader is the next try.
        }
        ClassLoader loader = VistaFixNeutralizer.class.getClassLoader();
        if (loader == null) {
            return null;
        }
        try (InputStream in = loader.getResourceAsStream(MIXIN_CLASS)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void logConfigNames(List<String> names) {
        if (names.isEmpty()) {
            LOGGER.info("Vista Mac Fix: no mixin config was visible from the loaders checked");
            return;
        }
        LOGGER.info("Vista Mac Fix: mixin configs visible: {}", names);
    }

    private static int stripConfigs(List<String> names) throws Exception {
        int removed = 0;
        for (Object handle : liveConfigHandles(names)) {
            String name = configName(handle);
            if (!CONFIG_NAME.equals(name)) {
                continue;
            }
            Object mixinConfig = handle.getClass().getMethod("getConfig").invoke(handle);
            if (mixinConfig != null) {
                removed += stripOne(mixinConfig);
            }
        }
        return removed;
    }

    private static List<Object> liveConfigHandles(List<String> names) {
        List<Object> handles = new ArrayList<>();
        Set<Object> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (ClassLoader loader : loaders()) {
            collectConfigs(loader, handles, seen, names);
        }
        return handles;
    }

    private static List<ClassLoader> loaders() {
        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<>();
        try {
            ClassLoader launch = FMLLoader.class.getClassLoader();
            if (launch != null) {
                loaders.add(launch);
            }
        } catch (Throwable ignored) {
            // The plugin can run before FML is initialized. Other loaders still apply.
        }
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            loaders.add(context);
        }
        ClassLoader own = VistaFixNeutralizer.class.getClassLoader();
        if (own != null) {
            loaders.add(own);
        }
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (system != null) {
            loaders.add(system);
        }
        return new ArrayList<>(loaders);
    }

    private static void collectConfigs(ClassLoader loader, List<Object> handles, Set<Object> seen, List<String> names) {
        try {
            Class<?> mixins = Class.forName("org.spongepowered.asm.mixin.Mixins", false, loader);
            Object set = mixins.getMethod("getConfigs").invoke(null);
            addHandles(set, handles, seen, names);
        } catch (Throwable ignored) {
            // This loader does not have Mixin. The next one might.
        }
        try {
            Class<?> configClass = Class.forName("org.spongepowered.asm.mixin.transformer.Config", false, loader);
            Field all = configClass.getDeclaredField("allConfigs");
            all.setAccessible(true);
            addHandles(all.get(null), handles, seen, names);
        } catch (Throwable ignored) {
            // allConfigs is a fallback when Mixins.getConfigs is a different copy.
        }
    }

    private static void addHandles(Object container, List<Object> handles, Set<Object> seen, List<String> names) {
        Collection<?> values = null;
        if (container instanceof Map<?, ?> map) {
            values = map.values();
        } else if (container instanceof Collection<?> collection) {
            values = collection;
        }
        if (values == null) {
            return;
        }
        for (Object handle : values) {
            if (handle == null || !seen.add(handle)) {
                continue;
            }
            handles.add(handle);
            String name = configName(handle);
            if (name != null && !names.contains(name)) {
                names.add(name);
            }
        }
    }

    private static String configName(Object handle) {
        try {
            Object name = handle.getClass().getMethod("getName").invoke(handle);
            return name instanceof String text ? text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static int stripOne(Object mixinConfig) throws Exception {
        Class<?> type = mixinConfig.getClass();
        Map<String, List<Object>> mapping = (Map<String, List<Object>>) readField(type, mixinConfig, "mixinMapping");
        List<Object> mixins = (List<Object>) readField(type, mixinConfig, "mixins");
        List<Object> pending = (List<Object>) readField(type, mixinConfig, "pendingMixins");
        List<String> names = new ArrayList<>();
        int classNamesRemoved = 0;
        for (String field : new String[] {"mixinClasses", "mixinClassesClient", "mixinClassesServer"}) {
            Object value = readField(type, mixinConfig, field);
            if (value instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof String name) {
                        names.add(name);
                    }
                }
                int before = list.size();
                ((List<String>) value).removeIf(VistaFixNeutralizer::isMixinClassName);
                classNamesRemoved += before - list.size();
            }
        }
        return classNamesRemoved + removeMatching(mapping, mixins, pending, names, VistaFixNeutralizer::isBrokenInfo, VistaFixNeutralizer::isMixinClassName);
    }

    private static boolean brokenMixinStillQueued() {
        try {
            for (Object handle : liveConfigHandles(new ArrayList<>())) {
                if (!CONFIG_NAME.equals(configName(handle))) {
                    continue;
                }
                Object mixinConfig = handle.getClass().getMethod("getConfig").invoke(handle);
                if (mixinConfig == null) {
                    continue;
                }
                Object mapping = readField(mixinConfig.getClass(), mixinConfig, "mixinMapping");
                if (!(mapping instanceof Map<?, ?> map)) {
                    continue;
                }
                for (Object list : map.values()) {
                    if (!(list instanceof List<?> mixins)) {
                        continue;
                    }
                    for (Object info : mixins) {
                        if (isBrokenInfo(info)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable error) {
            LOGGER.warn("Vista Mac Fix: could not confirm vistafix was unhooked", error);
            return true;
        }
        return false;
    }

    private static boolean isBrokenInfo(Object info) {
        if (info == null) {
            return false;
        }
        try {
            Method getName = info.getClass().getMethod("getName");
            Method getClassName = info.getClass().getMethod("getClassName");
            String name = String.valueOf(getName.invoke(info));
            String className = String.valueOf(getClassName.invoke(info));
            if (!isMixinClassName(name) && !isMixinClassName(className)) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }
        try {
            Object bytes = info.getClass().getMethod("getClassBytes").invoke(info);
            if (!(bytes instanceof byte[] raw)) {
                return true;
            }
            return bytesCallMissingFrameBuilder(raw);
        } catch (Throwable ignored) {
            return true;
        }
    }

    static boolean isMixinClassName(String name) {
        if (name == null) {
            return false;
        }
        return "TvRendererMixin".equals(name)
                || name.endsWith(".TvRendererMixin")
                || name.endsWith("/TvRendererMixin");
    }

    private static Object readField(Class<?> type, Object target, String name) throws Exception {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException missing) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private static void logOnce(String message) {
        if (LOGGED.compareAndSet(false, true)) {
            LOGGER.info(message);
        }
    }

    static int indexOf(byte[] haystack, byte[] needle) {
        if (haystack == null || needle == null || needle.length == 0 || haystack.length < needle.length) {
            return -1;
        }
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
