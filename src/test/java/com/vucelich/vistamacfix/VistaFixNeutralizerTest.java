package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class VistaFixNeutralizerTest {
    private static final String GOOD = "(FLnet/minecraft/client/renderer/MultiBufferSource;Z"
            + "Lnet/mehvahdjukaar/moonlight/api/util/math/Vec2i;"
            + "Lnet/mehvahdjukaar/moonlight/api/util/math/Vec2i;"
            + "IZLnet/mehvahdjukaar/vista/common/tv/IntAnimationState;"
            + "Lnet/mehvahdjukaar/vista/common/tv/IntAnimationState;Z)"
            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;";

    @Test
    void brokenMarkerMatchesVistafixAndNotAHarmlessClass() throws Exception {
        var resource = VistaFixNeutralizerTest.class.getResource(
                "/net/mehvahdjukaar/vistafix/mixin/TvRendererMixin.class");
        assertTrue(resource != null, "vistafix mixin on the test classpath");
        byte[] bytes;
        try (var in = resource.openStream()) {
            bytes = in.readAllBytes();
        }
        assertTrue(VistaFixNeutralizer.bytesCallMissingFrameBuilder(bytes));
        assertFalse(VistaFixNeutralizer.bytesCallMissingFrameBuilder(
                "getVideoFrameBuilder Vec2i".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(VistaFixNeutralizer.isMixinClassName("TvRendererMixin"));
        assertTrue(VistaFixNeutralizer.isMixinClassName("net.mehvahdjukaar.vistafix.mixin.TvRendererMixin"));
        assertFalse(VistaFixNeutralizer.isMixinClassName("OtherMixin"));
    }

    @Test
    void removeMatchingDropsOnlyTheBrokenMixin() {
        Map<String, List<Object>> mapping = new LinkedHashMap<>();
        List<Object> target = new ArrayList<>();
        target.add("TvRendererMixin");
        target.add("KeepMe");
        mapping.put("net/mehvahdjukaar/vista/client/renderer/TvBlockEntityRenderer", target);
        List<Object> mixins = new ArrayList<>(List.of("TvRendererMixin", "KeepMe"));
        List<Object> pending = new ArrayList<>(List.of("TvRendererMixin"));
        List<String> names = new ArrayList<>(List.of("TvRendererMixin", "OtherMixin"));

        int removed = VistaFixNeutralizer.removeMatching(
                mapping,
                mixins,
                pending,
                names,
                value -> "TvRendererMixin".equals(value),
                VistaFixNeutralizer::isMixinClassName
        );

        assertEquals(3, removed);
        assertEquals(List.of("KeepMe"), target);
        assertEquals(List.of("KeepMe"), mixins);
        assertTrue(pending.isEmpty());
        assertEquals(List.of("OtherMixin"), names);
    }

    @Test
    void ourRendererCallsTheVista4411Method() throws Exception {
        Path classFile = Path.of("build/classes/java/main/com/vucelich/vistamacfix/ContraptionTvScreen.class");
        assertTrue(Files.isRegularFile(classFile), "compile ContraptionTvScreen first");
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(classFile)).accept(node, 0);
        boolean good = false;
        boolean broken = false;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) {
                    continue;
                }
                if (!"getVideoFrameBuilder".equals(call.name)) {
                    continue;
                }
                if (GOOD.equals(call.desc)) {
                    good = true;
                }
                if (call.desc.contains("ZIIIZ")) {
                    broken = true;
                }
            }
        }
        assertTrue(good, "expected Vista 4.4.11 getVideoFrameBuilder");
        assertFalse(broken);
    }

    @Test
    void renderedClassScanSeesTheBrokenCall() {
        ClassNode clean = new ClassNode();
        clean.methods = new ArrayList<>();
        MethodNode fine = new MethodNode();
        fine.instructions = new org.objectweb.asm.tree.InsnList();
        fine.instructions.add(new MethodInsnNode(
                org.objectweb.asm.Opcodes.INVOKEINTERFACE,
                "net/mehvahdjukaar/vista/client/video_source/IVideoSource",
                "getVideoFrameBuilder",
                GOOD,
                true
        ));
        clean.methods.add(fine);
        assertFalse(VistaFixNeutralizer.classCallsMissingFrameBuilder(clean));

        ClassNode broken = new ClassNode();
        broken.methods = new ArrayList<>();
        MethodNode bad = new MethodNode();
        bad.instructions = new org.objectweb.asm.tree.InsnList();
        bad.instructions.add(new MethodInsnNode(
                org.objectweb.asm.Opcodes.INVOKEINTERFACE,
                "net/mehvahdjukaar/vista/client/video_source/IVideoSource",
                "getVideoFrameBuilder",
                "(FLnet/minecraft/client/renderer/MultiBufferSource;ZIIIZLnet/mehvahdjukaar/vista/common/tv/IntAnimationState;Lnet/mehvahdjukaar/vista/common/tv/IntAnimationState;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
                true
        ));
        broken.methods.add(bad);
        assertTrue(VistaFixNeutralizer.classCallsMissingFrameBuilder(broken));
    }
}
