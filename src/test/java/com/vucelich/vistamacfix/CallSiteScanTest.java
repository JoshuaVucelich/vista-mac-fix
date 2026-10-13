package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The pack jars may call glCopyBufferSubData from exactly two methods:
 * Sodium's command list and Sable's bucket resize. Those are the mixin targets.
 */
class CallSiteScanTest {
    private static final Path MODS = Path.of("/tmp/scanmods");
    private static final byte[] NAME = "glCopyBufferSubData".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NNAME = "nglCopyBufferSubData".getBytes(StandardCharsets.US_ASCII);

    @Test
    void everyPackCallSiteIsRedirected() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(MODS), "pack jars are not staged at " + MODS);
        List<String> sites = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();
        try (var listing = Files.list(MODS)) {
            for (Path jar : listing.filter(path -> path.toString().endsWith(".jar")).sorted().toList()) {
                scanJar(jar, jar.getFileName().toString(), sites, uncovered, 0);
            }
        }
        String report = String.join("\n", sites);
        System.out.println(report.isEmpty() ? "(no glCopyBufferSubData call sites)" : report);
        Files.writeString(Path.of("/tmp/copybuffer-sites.txt"), report + "\n");
        assertTrue(sites.stream().anyMatch(line -> line.contains("GLRenderDevice")), report);
        assertTrue(sites.stream().anyMatch(line -> line.contains("BucketRenderBuffer")), report);
        assertTrue(uncovered.isEmpty(), "uncovered call sites:\n" + String.join("\n", uncovered));
        assertEquals(sites.size(), sites.stream().filter(line -> line.startsWith("COVERED ")).count());
    }

    private static void scanJar(Path jar, String label, List<String> sites, List<String> uncovered, int depth) throws IOException {
        if (depth > 4) {
            return;
        }
        try (JarFile file = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    byte[] bytes = read(file, entry);
                    if (!contains(bytes, NAME) && !contains(bytes, NNAME)) {
                        continue;
                    }
                    inspect(label, name, bytes, sites, uncovered);
                } else if (name.endsWith(".jar")) {
                    Path nested = Files.createTempFile("vista-scan-", ".jar");
                    try {
                        Files.write(nested, read(file, entry));
                        scanJar(nested, label + "!" + name, sites, uncovered, depth + 1);
                    } finally {
                        Files.deleteIfExists(nested);
                    }
                }
            }
        }
    }

    private static void inspect(String label, String classFile, byte[] bytes, List<String> sites, List<String> uncovered) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        List<String> found = new ArrayList<>();
        collect(node, found);
        boolean covered = node.name.endsWith("GLRenderDevice$ImmediateCommandList")
                || node.name.endsWith("BucketRenderBuffer");
        for (String site : found) {
            String line = (covered ? "COVERED " : "UNCOVERED ") + label + " " + node.name + " " + site;
            sites.add(line);
            if (!covered) {
                uncovered.add(line);
            }
        }
    }

    private static void collect(ClassNode node, List<String> into) {
        if (node.methods == null) {
            return;
        }
        for (MethodNode method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call
                        && call.name.contains("CopyBufferSubData")) {
                    into.add(method.name + method.desc + " -> " + call.owner + "." + call.name + call.desc);
                }
            }
        }
    }

    private static byte[] read(JarFile file, JarEntry entry) throws IOException {
        try (InputStream input = file.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
