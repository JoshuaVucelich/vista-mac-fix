package com.vucelich.vistamacfix.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Rewrites every {@code glCopyBufferSubData} caller before the class is
 * defined. With {@code -Dvista_mac_fix.trap=true} the LWJGL methods themselves
 * throw, so a caller that still reaches the driver fails the run.
 */
public final class GlCopyTrapAgent {
    static final String REAL_CALL_REACHED = "real glCopyBufferSubData was reached";
    static final String FALLBACK_OWNER = "com/vucelich/vistamacfix/BufferCopyFallback";
    static final String DESC = "(IIJJJ)V";
    private static final Set<String> OWNERS = Set.of(
            "org/lwjgl/opengl/GL31C",
            "org/lwjgl/opengl/GL31",
            "org/lwjgl/opengl/ARBCopyBuffer"
    );
    private static final Set<String> NAMES = Set.of("glCopyBufferSubData", "nglCopyBufferSubData");
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    private GlCopyTrapAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    private static void install(Instrumentation instrumentation) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        System.setProperty("vista_mac_fix.agent", "true");
        instrumentation.addTransformer(new Rewriter(), true);
        System.err.println("Vista Mac Fix agent installed");
    }

    static byte[] rewrite(String internalName, byte[] classfile) {
        if (classfile == null || internalName == null || skip(internalName)) {
            return null;
        }
        if (!containsCopyName(classfile)) {
            return null;
        }
        ClassNode node = new ClassNode();
        new ClassReader(classfile).accept(node, 0);
        int rewritten = redirect(node);
        byte[] trapped = null;
        if (Boolean.getBoolean("vista_mac_fix.trap") && isGlTarget(internalName)) {
            trapped = trapMethods(node);
        }
        if (rewritten == 0 && trapped == null) {
            return null;
        }
        if (rewritten > 0) {
            System.err.println("Vista Mac Fix: redirected " + rewritten + " glCopyBufferSubData call"
                    + (rewritten == 1 ? "" : "s") + " in " + internalName.replace('/', '.'));
        }
        if (trapped != null) {
            System.err.println("Vista Mac Fix trapped " + internalName.replace('/', '.'));
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    static int redirect(ClassNode node) {
        if (node == null || node.methods == null || skip(node.name)) {
            return 0;
        }
        int rewritten = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode call)) {
                    continue;
                }
                if (!OWNERS.contains(call.owner) || !NAMES.contains(call.name) || !DESC.equals(call.desc)) {
                    continue;
                }
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = FALLBACK_OWNER;
                call.name = "copy";
                call.desc = DESC;
                call.itf = false;
                rewritten++;
            }
        }
        return rewritten;
    }

    private static byte[] trapMethods(ClassNode node) {
        boolean changed = false;
        if (node.methods != null) {
            for (MethodNode method : node.methods) {
                if (NAMES.contains(method.name) && DESC.equals(method.desc)) {
                    replaceWithThrow(method);
                    changed = true;
                }
            }
        }
        return changed ? new byte[] {1} : null;
    }

    static boolean skip(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return true;
        }
        String name = internalName.replace('.', '/');
        return name.startsWith("com/vucelich/vistamacfix/")
                || name.startsWith("java/")
                || name.startsWith("jdk/")
                || name.startsWith("sun/")
                || (name.startsWith("org/lwjgl/") && !isGlTarget(name));
    }

    private static boolean isGlTarget(String internalName) {
        String name = internalName.replace('.', '/');
        return OWNERS.contains(name);
    }

    private static boolean containsCopyName(byte[] classfile) {
        String latin = new String(classfile, java.nio.charset.StandardCharsets.ISO_8859_1);
        return latin.contains("glCopyBufferSubData") || latin.contains("nglCopyBufferSubData");
    }

    private static void replaceWithThrow(MethodNode method) {
        method.access &= ~Opcodes.ACC_NATIVE;
        method.instructions = new InsnList();
        method.tryCatchBlocks = null;
        method.localVariables = null;
        method.exceptions = null;
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new LdcInsnNode(REAL_CALL_REACHED));
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

    private static final class Rewriter implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            return rewrite(className, classfileBuffer);
        }
    }
}
