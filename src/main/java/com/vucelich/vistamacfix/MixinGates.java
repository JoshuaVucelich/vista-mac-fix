package com.vucelich.vistamacfix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Decides whether a mixin may apply. Missing Vista, or a Vista build whose
 * live-feed method no longer matches, leaves the game on the original path.
 */
public final class MixinGates {
    public static final String VISTA_MOD = "net/mehvahdjukaar/vista/VistaMod.class";

    public static final String RENDER_44 =
            "(Lnet/mehvahdjukaar/vista/client/textures/LiveFeedTexture;Lnet/mehvahdjukaar/vista/common/view_finder/ViewFinderBlockEntity;)V";

    public static final String RENDER_55 =
            "(Lnet/mehvahdjukaar/vista/client/textures/perspective/PerspectiveTexture;Lnet/mehvahdjukaar/vista/common/view_finder/ViewFinderBlockEntity;)V";

    private MixinGates() {
    }

    public static boolean shouldApply(ClassLoader loader, String targetClassName, String requiredDescriptor) {
        if (loader == null || requiredDescriptor == null) {
            return false;
        }
        if (loader.getResource(VISTA_MOD) == null) {
            return false;
        }
        String path = targetClassName.replace('.', '/') + ".class";
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) {
                return false;
            }
            byte[] bytes = in.readAllBytes();
            return indexOf(bytes, requiredDescriptor.getBytes(StandardCharsets.UTF_8)) >= 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    static int indexOf(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) {
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
