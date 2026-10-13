package com.vucelich.vistamacfix.launch;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class LaunchFiles {
    static final String AGENT_ENTRY = "META-INF/vista-mac-fix-agent.jar";

    private LaunchFiles() {
    }

    static Path codeSource() {
        try {
            var source = LaunchFiles.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            URL location = source.getLocation();
            URI uri = location.toURI();
            if ("jar".equalsIgnoreCase(uri.getScheme())) {
                String specific = uri.getSchemeSpecificPart();
                int bang = specific.indexOf('!');
                if (bang >= 0) {
                    specific = specific.substring(0, bang);
                }
                return Path.of(URI.create(specific));
            }
            return Path.of(uri);
        } catch (Exception error) {
            error.printStackTrace(System.err);
            return null;
        }
    }

    static Path extractAgent(Path root) throws Exception {
        Path destination = Files.createTempFile("vista-mac-fix-agent", ".jar");
        destination.toFile().deleteOnExit();
        Path nested = root.resolve(AGENT_ENTRY);
        if (Files.isRegularFile(nested)) {
            Files.copy(nested, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        }
        if (Files.isRegularFile(root)) {
            try (ZipFile zip = new ZipFile(root.toFile())) {
                ZipEntry entry = zip.getEntry(AGENT_ENTRY);
                if (entry == null) {
                    return null;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                return destination;
            }
        }
        return null;
    }
}
