package com.vucelich.vistamacfix.mixin;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vucelich.vistamacfix.AppleGl;
import com.vucelich.vistamacfix.BufferCopyFallback;

/**
 * Sodium 0.8.13 copies staging bytes with {@code GL31C.glCopyBufferSubData}
 * inside {@code GLRenderDevice$ImmediateCommandList.copyBufferSubData}.
 * {@code require = 0} so a missing method does not crash.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice$ImmediateCommandList", remap = false)
public class SodiumCopyBufferMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    static {
        LOGGER.info("Vista Mac Fix: redirect applied to Sodium GLRenderDevice$ImmediateCommandList.copyBufferSubData");
    }

    @WrapOperation(
            method = "copyBufferSubData",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL31C;glCopyBufferSubData(IIJJJ)V"
            ),
            require = 0
    )
    private void vistaMacFix$copy(
            int readTarget,
            int writeTarget,
            long readOffset,
            long writeOffset,
            long size,
            Operation<Void> original
    ) {
        if (!AppleGl.isActive()) {
            original.call(readTarget, writeTarget, readOffset, writeOffset, size);
            return;
        }
        if (LOGGED.compareAndSet(false, true)) {
            LOGGER.info("Vista Mac Fix: redirected Sodium GL31C.glCopyBufferSubData to the CPU copy");
        }
        BufferCopyFallback.copy(readTarget, writeTarget, readOffset, writeOffset, size);
    }
}
