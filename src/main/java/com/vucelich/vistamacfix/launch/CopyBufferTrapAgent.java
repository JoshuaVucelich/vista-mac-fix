package com.vucelich.vistamacfix.launch;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.vucelich.vistamacfix.BufferCopyFallback;
import com.vucelich.vistamacfix.CopyBufferRewriter;

/**
 * Test hook. Started with {@code -javaagent:vista_mac_fix.jar} and
 * {@code -Dvista_mac_fix.trap=true}, it turns the real LWJGL
 * {@code glCopyBufferSubData} methods into a throw. A client run that still
 * reaches the driver function fails loudly instead of passing silently.
 */
public final class CopyBufferTrapAgent {
    private static final String[] TARGETS = {
            "org/lwjgl/opengl/GL31C",
            "org/lwjgl/opengl/GL31",
            "org/lwjgl/opengl/ARBCopyBuffer"
    };

    private CopyBufferTrapAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        if (!Boolean.getBoolean("vista_mac_fix.trap")) {
            return;
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return trap(className, classfileBuffer);
            }
        }, false);
    }

    public static byte[] trap(String internalName, byte[] classfile) {
        if (classfile == null || !isTarget(internalName)) {
            return null;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classfile).accept(node, 0);
        boolean changed = false;
        if (node.methods != null) {
            for (MethodNode method : node.methods) {
                if (CopyBufferRewriter.NAMES.contains(method.name) && CopyBufferRewriter.DESC.equals(method.desc)) {
                    replaceWithThrow(method);
                    changed = true;
                }
            }
        }
        if (!changed) {
            return null;
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isTarget(String internalName) {
        if (internalName == null) {
            return false;
        }
        String name = internalName.replace('.', '/');
        for (String target : TARGETS) {
            if (target.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void replaceWithThrow(MethodNode method) {
        method.access &= ~Opcodes.ACC_NATIVE;
        method.instructions = new InsnList();
        method.tryCatchBlocks = null;
        method.localVariables = null;
        method.exceptions = null;
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new LdcInsnNode(BufferCopyFallback.REAL_CALL_REACHED));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/IllegalStateException",
                "<init>",
                "(Ljava/lang/String;)V",
                false
        ));
        method.instructions.add(new InsnNode(Opcodes.ATHROW));
        method.maxStack = 3;
        method.maxLocals = 8;
    }
}
