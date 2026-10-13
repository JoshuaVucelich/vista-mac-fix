package com.vucelich.vistamacfix;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Bytecode inserted at the head of Vista's live-feed render. */
public final class LiveFeedSkip {
    private LiveFeedSkip() {
    }

    public static void insert(MethodNode method) {
        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        int textureLocal = isStatic ? 0 : 1;
        LabelNode continueRender = new LabelNode();
        InsnList skip = new InsnList();
        skip.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/vucelich/vistamacfix/AppleGl",
                "isActive",
                "()Z",
                false
        ));
        skip.add(new JumpInsnNode(Opcodes.IFEQ, continueRender));
        skip.add(new VarInsnNode(Opcodes.ALOAD, textureLocal));
        skip.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/vucelich/vistamacfix/PlaceholderScreen",
                "show",
                "(Ljava/lang/Object;)V",
                false
        ));
        skip.add(new InsnNode(Opcodes.RETURN));
        skip.add(continueRender);
        method.instructions.insert(skip);
        if (method.maxStack < 1) {
            method.maxStack = 1;
        }
    }
}
