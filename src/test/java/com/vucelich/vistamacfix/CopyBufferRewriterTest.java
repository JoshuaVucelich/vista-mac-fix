package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.vucelich.vistamacfix.launch.CopyBufferTrapAgent;

class CopyBufferRewriterTest {
    @Test
    void redirectsEveryLwjglEntryPoint() {
        ClassNode node = new ClassNode();
        node.name = "dev/example/Upload";
        node.superName = "java/lang/Object";
        node.version = Opcodes.V21;
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "copy", "()V", null, null);
        method.instructions = new InsnList();
        for (String owner : CopyBufferRewriter.OWNERS) {
            for (String name : CopyBufferRewriter.NAMES) {
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, CopyBufferRewriter.DESC, false));
            }
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);

        int rewritten = CopyBufferRewriter.rewrite(node);
        assertEquals(CopyBufferRewriter.OWNERS.size() * CopyBufferRewriter.NAMES.size(), rewritten);
        for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() != Opcodes.RETURN) {
                assertFalse(CopyBufferRewriter.isCopyInvoke(call));
                if (call.name.equals(CopyBufferRewriter.FALLBACK_NAME)) {
                    assertEquals(CopyBufferRewriter.FALLBACK_OWNER, call.owner);
                    assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
                }
            }
        }
    }

    @Test
    void leavesTheFallbackClassAlone() throws Exception {
        byte[] bytes = BufferCopyFallback.class.getResourceAsStream("BufferCopyFallback$LwjglGl.class").readAllBytes();
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int nativeCalls = countCopyInvokes(node);
        assertTrue(nativeCalls > 0);
        assertEquals(0, CopyBufferRewriter.rewrite(node));
        assertEquals(nativeCalls, countCopyInvokes(node));
    }

    @Test
    void trapAgentReplacesTheNativeMethodWithAThrow() {
        ClassNode original = new ClassNode();
        original.version = Opcodes.V21;
        original.name = "org/lwjgl/opengl/GL31C";
        original.superName = "java/lang/Object";
        original.access = Opcodes.ACC_PUBLIC;
        MethodNode nativeMethod = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE,
                "glCopyBufferSubData",
                CopyBufferRewriter.DESC,
                null,
                null
        );
        original.methods.add(nativeMethod);
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        original.accept(writer);

        byte[] trapped = CopyBufferTrapAgent.trap(original.name, writer.toByteArray());
        ClassNode node = new ClassNode();
        new ClassReader(trapped).accept(node, 0);
        MethodNode method = node.methods.get(0);
        assertEquals(0, method.access & Opcodes.ACC_NATIVE);
        boolean thrown = false;
        for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.ATHROW) {
                thrown = true;
            }
        }
        assertTrue(thrown);
        assertTrue(new String(trapped, java.nio.charset.StandardCharsets.ISO_8859_1)
                .contains(BufferCopyFallback.REAL_CALL_REACHED));
    }

    private static int countCopyInvokes(ClassNode node) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call && CopyBufferRewriter.isCopyInvoke(call)) {
                    count++;
                }
            }
        }
        return count;
    }
}
