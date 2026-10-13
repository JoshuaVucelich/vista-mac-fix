package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class ViewFinderStubTest {
    @Test
    void stillValidInvokesBlockPosDistToCenterSqr() {
        ClassNode node = new ClassNode();
        new ClassReader(ViewFinderStub.blockClass()).accept(node, 0);
        assertEquals(ViewFinderStub.BLOCK, node.name);
        MethodNode still = node.methods.stream().filter(method -> method.name.equals("stillValid")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/world/entity/player/Player;)Z", still.desc);
        boolean found = false;
        for (var insn = still.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && call.name.equals("distToCenterSqr")) {
                assertEquals("net/minecraft/core/BlockPos", call.owner);
                assertEquals("(Lnet/minecraft/core/Position;)D", call.desc);
                found = true;
            }
        }
        assertTrue(found);
    }
}
