package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class LiveFeedSkipTest {
    @Test
    void forcedMacPathReturnsBeforeTheOriginalRender() {
        MethodNode render = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "render",
                MixinGates.RENDER_44,
                null,
                null
        );
        render.instructions.add(new InsnNode(Opcodes.RETURN));
        render.maxStack = 0;

        LiveFeedSkip.insert(render);

        MethodInsnNode active = null;
        MethodInsnNode show = null;
        boolean sawReturnBeforeOriginal = false;
        int returns = 0;
        for (AbstractInsnNode insn = render.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call) {
                if ("isActive".equals(call.name)) {
                    active = call;
                }
                if ("show".equals(call.name)) {
                    show = call;
                }
            }
            if (insn.getOpcode() == Opcodes.RETURN) {
                returns++;
                if (returns == 1) {
                    sawReturnBeforeOriginal = true;
                }
            }
        }
        assertTrue(active != null && "com/vucelich/vistamacfix/AppleGl".equals(active.owner));
        assertTrue(show != null && "com/vucelich/vistamacfix/PlaceholderScreen".equals(show.owner));
        assertEquals("(Ljava/lang/Object;)V", show.desc);
        assertTrue(sawReturnBeforeOriginal);
        assertEquals(2, returns);
        assertTrue(render.maxStack >= 1);
    }
}
