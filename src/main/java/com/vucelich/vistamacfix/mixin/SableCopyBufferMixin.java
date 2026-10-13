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
 * Sable 2.0.3 grows {@code BucketRenderBuffer} with
 * {@code ARBCopyBuffer.glCopyBufferSubData}. {@code require = 0} so a missing
 * method does not crash.
 */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.fancy.BucketRenderBuffer", remap = false)
public class SableCopyBufferMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    static {
        LOGGER.info("Vista Mac Fix: redirect applied to Sable BucketRenderBuffer.resize");
    }

    @WrapOperation(
            method = "resize",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/ARBCopyBuffer;glCopyBufferSubData(IIJJJ)V"
            ),
            require = 0
    )
    private void vistaMacFix$resizeCopy(
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
            LOGGER.info("Vista Mac Fix: redirected Sable ARBCopyBuffer.glCopyBufferSubData to the CPU copy");
        }
        BufferCopyFallback.copy(readTarget, writeTarget, readOffset, writeOffset, size);
    }
}
