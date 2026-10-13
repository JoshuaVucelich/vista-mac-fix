package com.vucelich.vistamacfix;

import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rewrites every {@code glCopyBufferSubData} / {@code nglCopyBufferSubData}
 * invoke on the LWJGL entry points into {@link BufferCopyFallback#copy}.
 * Call sites in this mod are left alone so the non-Mac path can still reach
 * the real GL function.
 */
public final class CopyBufferRewriter {
    public static final String FALLBACK_OWNER = "com/vucelich/vistamacfix/BufferCopyFallback";
    public static final String FALLBACK_NAME = "copy";
    public static final String DESC = "(IIJJJ)V";

    public static final Set<String> OWNERS = Set.of(
            "org/lwjgl/opengl/GL31C",
            "org/lwjgl/opengl/GL31",
            "org/lwjgl/opengl/ARBCopyBuffer"
    );

    public static final Set<String> NAMES = Set.of(
            "glCopyBufferSubData",
            "nglCopyBufferSubData"
    );

    private CopyBufferRewriter() {
    }

    public static boolean skipClass(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return true;
        }
        return internalName.startsWith("com/vucelich/vistamacfix/")
                || internalName.startsWith("org/lwjgl/")
                || internalName.startsWith("java/")
                || internalName.startsWith("jdk/")
                || internalName.startsWith("sun/");
    }

    public static boolean isCopyInvoke(AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode call)) {
            return false;
        }
        return OWNERS.contains(call.owner) && NAMES.contains(call.name) && DESC.equals(call.desc);
    }

    /** @return how many invokes were redirected */
    public static int rewrite(ClassNode node) {
        if (node == null || node.methods == null || skipClass(node.name)) {
            return 0;
        }
        int rewritten = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode call) || !isCopyInvoke(call)) {
                    continue;
                }
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = FALLBACK_OWNER;
                call.name = FALLBACK_NAME;
                call.desc = DESC;
                call.itf = false;
                rewritten++;
            }
        }
        return rewritten;
    }
}
