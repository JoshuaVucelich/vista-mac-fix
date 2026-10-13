package com.vucelich.vistamacfix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Paints Vista's offscreen feed targets a flat color and never renders the world.
 * That avoids the Apple driver stub for {@code glCopyBufferSubData}.
 */
public final class PlaceholderScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean LOGGED = new AtomicBoolean();
    private static final float RED = 0.10f;
    private static final float GREEN = 0.13f;
    private static final float BLUE = 0.18f;

    private PlaceholderScreen() {
    }

    public static void show(Object texture) {
        if (LOGGED.compareAndSet(false, true)) {
            LOGGER.info("Vista Mac Fix: skipping the live viewfinder render and showing a placeholder screen");
        }
        if (texture == null) {
            return;
        }
        int previousFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        float[] clear = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clear);
        try {
            ensureTargets(texture);
            clearTarget(readField(texture, "writeTarget"));
            clearTarget(readField(texture, "readTarget"));
        } catch (Throwable error) {
            if (LOGGED.get()) {
                LOGGER.warn("Vista Mac Fix: placeholder paint failed; the screen stays blank instead of crashing", error);
            }
        } finally {
            GL11.glClearColor(clear[0], clear[1], clear[2], clear[3]);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }

    private static void ensureTargets(Object texture) {
        if (readField(texture, "writeTarget") != null && readField(texture, "readTarget") != null) {
            return;
        }
        try {
            Method getId = texture.getClass().getMethod("getId");
            getId.invoke(texture);
        } catch (ReflectiveOperationException ignored) {
            // Targets may already exist, or this Vista build names them differently.
        }
    }

    private static void clearTarget(Object target) throws ReflectiveOperationException {
        if (target == null) {
            return;
        }
        int fbo = intField(target, "frameBufferId");
        int width = intField(target, "width");
        int height = intField(target, "height");
        if (fbo == 0 || width <= 0 || height <= 0) {
            return;
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, width, height);
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(RED, GREEN, BLUE, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    private static Object readField(Object instance, String name) {
        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static int intField(Object instance, String name) throws ReflectiveOperationException {
        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(instance);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
