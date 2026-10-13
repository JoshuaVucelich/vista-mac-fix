package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MixinGatesTest {
    @TempDir
    Path temp;

    @Test
    void missingVistaFailsOpen() throws IOException {
        writeClass(temp, "net/mehvahdjukaar/vista/client/renderer/VistaLevelRenderer.class", MixinGates.RENDER_44);
        try (URLClassLoader loader = loader(temp)) {
            assertFalse(MixinGates.shouldApply(
                    loader,
                    "net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer",
                    MixinGates.RENDER_44
            ));
        }
    }

    @Test
    void vista44MethodApplies() throws IOException {
        writeClass(temp, MixinGates.VISTA_MOD, "vista");
        writeClass(temp, "net/mehvahdjukaar/vista/client/renderer/VistaLevelRenderer.class", MixinGates.RENDER_44);
        try (URLClassLoader loader = loader(temp)) {
            assertTrue(MixinGates.shouldApply(
                    loader,
                    "net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer",
                    MixinGates.RENDER_44
            ));
            assertFalse(MixinGates.shouldApply(
                    loader,
                    "net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer",
                    MixinGates.RENDER_55
            ));
        }
    }

    @Test
    void shippedVistaJarsMatchTheSkip() throws IOException {
        Path root = Path.of("/tmp/vista-src");
        Path vista44 = root.resolve("vista-4.4.11.jar");
        Path vista55 = root.resolve("vista-5.5.7.jar");
        if (!Files.exists(vista44) || !Files.exists(vista55)) {
            return;
        }
        assertTrue(jarContains(vista44, "net/mehvahdjukaar/vista/client/renderer/VistaLevelRenderer.class", MixinGates.RENDER_44));
        assertFalse(jarContains(vista44, "net/mehvahdjukaar/vista/client/renderer/VistaLevelRenderer.class", MixinGates.RENDER_55));
        assertTrue(jarContains(vista55, "net/mehvahdjukaar/vista/client/renderer/VistaLevelRenderer.class", MixinGates.RENDER_55));
        assertFalse(jarContains(vista55, "net/mehvahdjukaar/vista/client/renderer/VistaLevelRenderer.class", MixinGates.RENDER_44));
    }

    @Test
    void compiledMixinsNameTheVistaMethods() throws IOException {
        assertTrue(resourceContains("com/vucelich/vistamacfix/MixinGates.class", MixinGates.RENDER_44));
        assertTrue(resourceContains("com/vucelich/vistamacfix/MixinGates.class", MixinGates.RENDER_55));
        assertTrue(resourceContains("com/vucelich/vistamacfix/LiveFeedSkip.class", "PlaceholderScreen"));
        assertTrue(resourceContains("com/vucelich/vistamacfix/LiveFeedSkip.class", "isActive"));
    }

    private static boolean resourceContains(String path, String needle) throws IOException {
        try (var in = MixinGatesTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).contains(needle);
        }
    }

    private static boolean jarContains(Path jar, String entry, String needle) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var item = zip.getEntry(entry);
            if (item == null) {
                return false;
            }
            return new String(zip.getInputStream(item).readAllBytes(), StandardCharsets.ISO_8859_1).contains(needle);
        }
    }

    private static void writeClass(Path root, String name, String payload) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, payload);
    }

    private static URLClassLoader loader(Path root) throws IOException {
        return new URLClassLoader(new URL[]{root.toUri().toURL()}, null);
    }
}
