package com.vucelich.vistamacfix.launch;

import java.util.EnumSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vucelich.vistamacfix.CopyBufferRewriter;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

/**
 * Redirects every {@code glCopyBufferSubData} call site as its class is loaded.
 * LWJGL itself is already initialized by the early window, so the native method
 * cannot be replaced; the callers are rewritten instead.
 */
public final class CopyBufferLaunchPlugin implements ILaunchPluginService {
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private int classes;
    private int invokes;

    @Override
    public String name() {
        return "vista_mac_fix";
    }

    public CopyBufferLaunchPlugin() {
        System.err.println("Vista Mac Fix launch plugin constructed");
    }

    @Override
    public void initializeLaunch(ITransformerLoader transformerLoader, NamedPath[] specialPaths) {
        System.err.println("Vista Mac Fix launch plugin installed");
        LOGGER.info("Vista Mac Fix launch plugin installed");
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        if (isEmpty || classType == null || CopyBufferRewriter.skipClass(classType.getInternalName())) {
            return EnumSet.noneOf(Phase.class);
        }
        return EnumSet.of(Phase.BEFORE);
    }

    @Override
    public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
        int rewritten = CopyBufferRewriter.rewrite(classNode);
        if (rewritten == 0) {
            return ComputeFlags.NO_REWRITE;
        }
        classes++;
        invokes += rewritten;
        String message = "Vista Mac Fix: redirected " + rewritten + " glCopyBufferSubData call"
                + (rewritten == 1 ? "" : "s") + " in " + classType.getClassName();
        System.err.println(message);
        LOGGER.info("{} ({} classes, {} calls)", message, classes, invokes);
        // Same descriptor, so the existing stack map stays valid.
        return ComputeFlags.SIMPLE_REWRITE;
    }
}
