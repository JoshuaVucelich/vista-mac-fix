package com.vucelich.vistamacfix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Sable 2.0.3 mixins {@code ViewFinderAccess$Block}, which Vista 4.4.11 and
 * 5.5.x do not ship. The missing class is only a mixin warning, but Sable's
 * config is required, so the stub has the {@code stillValid} invoke Sable
 * wraps and nothing in the game calls it.
 */
public final class ViewFinderStub {
    public static final String OUTER = "net/mehvahdjukaar/vista/common/view_finder/ViewFinderAccess";
    public static final String BLOCK = OUTER + "$Block";

    private ViewFinderStub() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: ViewFinderStub <output-dir>");
        }
        write(Path.of(args[0]));
    }

    public static void write(Path root) throws IOException {
        Path outer = root.resolve(OUTER + ".class");
        Path block = root.resolve(BLOCK + ".class");
        Files.createDirectories(outer.getParent());
        Files.write(outer, outerClass());
        Files.write(block, blockClass());
    }

    static byte[] outerClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, OUTER, null, "java/lang/Object", null);
        writer.visitInnerClass(BLOCK, OUTER, "Block", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    static byte[] blockClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, BLOCK, null, "java/lang/Object", null);
        writer.visitInnerClass(BLOCK, OUTER, "Block", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        MethodVisitor still = writer.visitMethod(
                Opcodes.ACC_PUBLIC,
                "stillValid",
                "(Lnet/minecraft/world/entity/player/Player;)Z",
                null,
                null
        );
        still.visitCode();
        still.visitTypeInsn(Opcodes.NEW, "net/minecraft/core/BlockPos");
        still.visitInsn(Opcodes.DUP);
        still.visitInsn(Opcodes.ICONST_0);
        still.visitInsn(Opcodes.ICONST_0);
        still.visitInsn(Opcodes.ICONST_0);
        still.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/core/BlockPos", "<init>", "(III)V", false);
        still.visitVarInsn(Opcodes.ALOAD, 1);
        still.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/player/Player",
                "position",
                "()Lnet/minecraft/world/phys/Vec3;",
                false
        );
        still.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/core/BlockPos",
                "distToCenterSqr",
                "(Lnet/minecraft/core/Position;)D",
                false
        );
        still.visitInsn(Opcodes.DCONST_0);
        still.visitInsn(Opcodes.DCMPG);
        still.visitInsn(Opcodes.ICONST_1);
        still.visitInsn(Opcodes.IXOR);
        still.visitInsn(Opcodes.IRETURN);
        still.visitMaxs(5, 2);
        still.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
