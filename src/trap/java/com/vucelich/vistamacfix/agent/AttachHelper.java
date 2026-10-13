package com.vucelich.vistamacfix.agent;

import com.sun.tools.attach.VirtualMachine;

/**
 * A second JVM attaches the agent into the already-running client. Self-attach
 * is refused unless the client was started with {@code jdk.attach.allowAttachSelf}.
 */
public final class AttachHelper {
    private AttachHelper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: AttachHelper <pid> <agentJar>");
        }
        VirtualMachine vm = VirtualMachine.attach(args[0]);
        try {
            vm.loadAgent(args[1]);
        } finally {
            vm.detach();
        }
    }
}
