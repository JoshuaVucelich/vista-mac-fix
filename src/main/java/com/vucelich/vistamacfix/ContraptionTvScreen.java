package com.vucelich.vistamacfix;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec2;

/**
 * Draws a Vista 4.4.11 television when it sits on a moving Sable sub-level.
 * The screen quad uses that version's {@code getVideoFrameBuilder} (two
 * {@code Vec2i} arguments and a trailing boolean). Distance culling is skipped
 * because the block's plot position is not the ship's on-screen position.
 */
public final class ContraptionTvScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean LOGGED = new AtomicBoolean();
    private static final AtomicInteger DRAWS = new AtomicInteger();

    private ContraptionTvScreen() {
    }

    public static int draws() {
        return DRAWS.get();
    }

    /**
     * @return true when this call drew the screen and the original renderer
     *         must not also run
     */
    public static boolean render(
            TVBlockEntity television,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (ContraptionTvPolicy.disabled() || television == null || !screenOn(television, partialTick)) {
            return false;
        }
        pose.pushPose();
        try {
            Direction facing = television.getBlockState().getValue(TVBlock.FACING);
            Vec2i pixels = television.getScreenPixelSize();
            Vec2 center = television.getScreenBlockCenter();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            pose.translate(-center.x, center.y, -0.505);

            boolean paused = television.isPaused();
            Vec2i frameSize = frameSize(pixels);
            IVideoSource source = television.getVideoSource();
            if (source == null) {
                return false;
            }
            VertexConsumer consumer = source.getVideoFrameBuilder(
                    paused ? 1.0F : partialTick,
                    buffers,
                    !paused,
                    pixels,
                    frameSize,
                    television.getPlaybackTicks(),
                    paused,
                    television.fadeAnimation,
                    television.endermanAnimation,
                    television.showsTime()
            );
            if (consumer == null) {
                return false;
            }
            float halfWidth = pixels.x() / 32.0F;
            float halfHeight = pixels.y() / 32.0F;
            quad(consumer, pose, -halfWidth, -halfHeight, halfWidth, halfHeight, skyLight(television, facing, packedLight));
            DRAWS.incrementAndGet();
            if (LOGGED.compareAndSet(false, true)) {
                LOGGER.info("Vista Mac Fix: drew a Vista television on a Sable contraption");
            }
            return true;
        } catch (Throwable error) {
            ContraptionTvPolicy.onRenderFailure(error);
            return false;
        } finally {
            pose.popPose();
        }
    }

    private static boolean screenOn(TVBlockEntity television, float partialTick) {
        try {
            return television.isScreenOn(partialTick);
        } catch (Throwable error) {
            ContraptionTvPolicy.onRenderFailure(error);
            return false;
        }
    }

    private static Vec2i frameSize(Vec2i pixels) {
        try {
            if (Boolean.TRUE.equals(ClientConfigs.SCALE_PIXELS.get())) {
                return pixels;
            }
        } catch (Throwable ignored) {
            return pixels;
        }
        return TVBlockEntity.MIN_SCREEN_PIXEL_SIZE_VEC;
    }

    private static int skyLight(TVBlockEntity television, Direction facing, int fallback) {
        Level level = television.getLevel();
        if (level == null) {
            return fallback;
        }
        try {
            int sky = level.getBrightness(LightLayer.SKY, television.getBlockPos().relative(facing));
            return LightTexture.pack(15, sky);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack pose,
            float x0,
            float y0,
            float x1,
            float y1,
            int light
    ) {
        int block = light & 0xFFFF;
        int sky = (light >> 16) & 0xFFFF;
        Vector3f normal = new Vector3f(0.0F, 0.0F, -1.0F);
        Matrix3f matrix = pose.last().normal();
        matrix.transform(normal);
        vertex(consumer, pose, x0, y1, 1.0F, 0.0F, block, sky, normal);
        vertex(consumer, pose, x1, y1, 0.0F, 0.0F, block, sky, normal);
        vertex(consumer, pose, x1, y0, 0.0F, 1.0F, block, sky, normal);
        vertex(consumer, pose, x0, y0, 1.0F, 1.0F, block, sky, normal);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack pose,
            float x,
            float y,
            float u,
            float v,
            int blockLight,
            int skyLight,
            Vector3f normal
    ) {
        consumer.addVertex(pose.last().pose(), x, y, 0.0F)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(blockLight, skyLight)
                .setNormal(normal.x, normal.y, normal.z);
    }
}
