package com.vucelich.vistamacfix.launch;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * The outer jar is a launch service, so the mods folder skips it. The playable
 * mod is the nested jar, added here the same way Sodium adds its nested mod.
 */
public final class ServiceModLocator implements IModFileCandidateLocator {
    static final String NESTED_MOD = "META-INF/jarjar/vista_mac_fix-mod.jar";

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        Path root = LaunchFiles.codeSource();
        if (root == null) {
            System.err.println("Vista Mac Fix locator: code source unavailable");
            return;
        }
        // The in-jar path is already marked located because this jar is a launch
        // service. Copy it out so discovery sees a new file.
        Path nested = root.resolve(NESTED_MOD);
        if (Files.isRegularFile(nested)) {
            try {
                addFile(pipeline, copyOut(nested));
            } catch (Exception error) {
                System.err.println("Vista Mac Fix locator failed to copy " + nested);
                error.printStackTrace(System.err);
            }
            return;
        }
        if (!Files.isRegularFile(root)) {
            return;
        }
        try (ZipFile zip = new ZipFile(root.toFile())) {
            ZipEntry entry = zip.getEntry(NESTED_MOD);
            if (entry == null) {
                return;
            }
            Path extracted = Files.createTempFile("vista_mac_fix-mod", ".jar");
            extracted.toFile().deleteOnExit();
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, extracted, StandardCopyOption.REPLACE_EXISTING);
            }
            addFile(pipeline, extracted);
        } catch (Exception error) {
            System.err.println("Vista Mac Fix locator failed to read " + root);
            error.printStackTrace(System.err);
        }
    }

    private static Path copyOut(Path nested) throws Exception {
        Path extracted = Files.createTempFile("vista_mac_fix-mod", ".jar");
        extracted.toFile().deleteOnExit();
        Files.copy(nested, extracted, StandardCopyOption.REPLACE_EXISTING);
        return extracted;
    }

    private static void addFile(IDiscoveryPipeline pipeline, Path jar) {
        pipeline.addPath(jar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
        System.err.println("Vista Mac Fix added mod " + jar);
    }

    @Override
    public String toString() {
        return "vista mac fix mod";
    }
}
