package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

class DistantHorizonsMixinContractTest {
    @Test
    void feedMixinIsHeadInjectRequireZeroRemapFalse() throws Exception {
        ClassNode node = read("com/vucelich/vistamacfix/mixin/DistantHorizonsFeedMixin.class");
        AnnotationNode mixin = annotation(node.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        assertNotNull(mixin, "class Mixin annotation");
        assertEquals(Boolean.FALSE, value(mixin, "remap"));

        MethodNode method = node.methods.stream()
                .filter(candidate -> candidate.name.contains("skipDhConfig"))
                .findFirst()
                .orElseThrow();
        AnnotationNode inject = annotation(method.visibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
        if (inject == null) {
            inject = annotation(method.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
        }
        assertNotNull(inject, "Inject annotation");
        assertEquals(List.of("decorateRenderWithoutLOD(Ljava/lang/Runnable;)Ljava/lang/Runnable;"), value(inject, "method"));
        assertEquals(Boolean.TRUE, value(inject, "cancellable"));
        assertEquals(0, value(inject, "require"));
        assertEquals(Boolean.FALSE, value(inject, "remap"));
        assertNotNull(value(inject, "at"));
    }

    @Test
    void mixinConfigListsTheFeedGuard() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/vistamacfix.mixins.json"));
        assertTrue(json.contains("DistantHorizonsFeedMixin"));
        assertTrue(json.contains("VistaFeedRateMixin"));
        assertTrue(json.contains("com.vucelich.vistamacfix.VistaMacFixMixinPlugin"));
    }

    private static ClassNode read(String path) throws Exception {
        Path file = Path.of("build/classes/java/main").resolve(path);
        assertTrue(Files.isRegularFile(file), file.toString());
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(file)).accept(node, 0);
        return node;
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String desc) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (desc.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation == null || annotation.values == null) {
            return null;
        }
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (name.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }
}
