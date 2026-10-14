package com.vucelich.vistamacfix;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A television on a Sable ship lives in a sub-level plot. Its block position
 * is not where the camera is, so Vista's distance cull hides the screen.
 */
public final class SableContraptions {
    private static final String SABLE = "dev.ryanhcode.sable.Sable";
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();
    private static volatile Method containingClient;
    private static volatile Object helper;
    private static volatile boolean unavailable;

    private SableContraptions() {
    }

    public static boolean contains(BlockEntity blockEntity) {
        if (blockEntity == null || unavailable) {
            return false;
        }
        try {
            Method method = containingClient;
            Object target = helper;
            if (method == null) {
                Class<?> sable = Class.forName(SABLE, false, SableContraptions.class.getClassLoader());
                target = sable.getField("HELPER").get(null);
                method = target.getClass().getMethod("getContainingClient", BlockEntity.class);
                helper = target;
                containingClient = method;
            }
            return method.invoke(target, blockEntity) != null;
        } catch (ClassNotFoundException missing) {
            unavailable = true;
            return false;
        } catch (Throwable error) {
            if (LOGGED_FAILURE.compareAndSet(false, true)) {
                LOGGER.warn("Vista Mac Fix: could not ask Sable whether a block is on a contraption", error);
            }
            return false;
        }
    }
}
