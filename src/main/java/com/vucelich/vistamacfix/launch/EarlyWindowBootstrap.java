package com.vucelich.vistamacfix.launch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;

/**
 * Runs before game classes load. The outer jar cannot register a launch plugin
 * (those are loaded only from the boot layer), so this attaches an agent that
 * rewrites {@code glCopyBufferSubData} as each caller class is defined.
 */
public final class EarlyWindowBootstrap implements GraphicsBootstrapper {
    @Override
    public String name() {
        return "vista_mac_fix";
    }

    @Override
    public void bootstrap(String[] arguments) {
        System.err.println("Vista Mac Fix early window bootstrap");
        if (Boolean.getBoolean("vista_mac_fix.agent")) {
            System.err.println("Vista Mac Fix agent already installed");
            return;
        }
        try {
            attachAgent();
        } catch (Throwable error) {
            System.err.println("Vista Mac Fix could not attach its agent. "
                    + "Start the client with -javaagent pointing at the extracted vista-mac-fix-agent.jar.");
            error.printStackTrace(System.err);
        }
    }

    private static void attachAgent() throws Exception {
        Path root = LaunchFiles.codeSource();
        if (root == null) {
            throw new IllegalStateException("code source unavailable");
        }
        Path agent = LaunchFiles.extractAgent(root);
        if (agent == null || !Files.isRegularFile(agent) || Files.size(agent) == 0) {
            System.err.println("Vista Mac Fix agent jar not packed in " + root);
            return;
        }
        Path java = javaBinary();
        String pid = Long.toString(ProcessHandle.current().pid());
        Process process = new ProcessBuilder(
                java.toString(),
                "--add-modules", "jdk.attach",
                "-cp", agent.toString(),
                "com.vucelich.vistamacfix.agent.AttachHelper",
                pid,
                agent.toString()
        ).redirectErrorStream(true).start();
        byte[] output = process.getInputStream().readAllBytes();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        int code = finished ? process.exitValue() : -1;
        String text = new String(output).trim();
        System.err.println("Vista Mac Fix agent attach exit " + code + (text.isEmpty() ? "" : " " + text));
        if (!finished || code != 0) {
            process.destroyForcibly();
            throw new IllegalStateException("agent attach failed: " + code + " " + text);
        }
    }

    private static Path javaBinary() {
        Path home = Path.of(System.getProperty("java.home"), "bin");
        Path java = home.resolve("java");
        if (Files.isExecutable(java)) {
            return java;
        }
        Path exe = home.resolve("java.exe");
        if (Files.isExecutable(exe)) {
            return exe;
        }
        return java;
    }
}
