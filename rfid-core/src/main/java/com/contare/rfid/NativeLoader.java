package com.contare.rfid;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * NativeLoader
 * <p>
 * - Loads native libraries packaged as resources (extracts to tempfile then System.load).
 * - Only loads resources whose extension matches the current OS.
 * - Ensures each resource is loaded only once per JVM (thread safe).
 */
public final class NativeLoader {

    private static final Logger logger = Logger.getLogger(NativeLoader.class);

    private static final String OS = detectOS();
    private static final String ARCH = detectArch();

    // Tracks successfully loaded resource keys (resource path normalized to start-with '/')
    private static final Set<String> _loaded = ConcurrentHashMap.newKeySet();

    // Per-resource locks to avoid double-loading races
    private static final ConcurrentMap<String, Object> _locks = new ConcurrentHashMap<>();

    private NativeLoader() {
        // no instantiation
    }

    /**
     * Load a resource from the classpath only if its filename extension is appropriate
     * for the current platform. Example resourcePath values:
     * - "files/libTagReader.so"
     * - "/files/UHFAPI.dll"
     * <p>
     * This method is idempotent: the same resource will be loaded at most once per JVM.
     *
     * @param resourcePath resource path inside the JAR/classpath (with or without leading '/')
     * @throws IOException if resource is not found or copy/load fails
     */
    public static void load(final String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "Resource path must not be null");

        final String normalized = normalizeResourcePath(resourcePath);

        // quick check: is this resource intended for this OS?
        if (!shouldLoadForCurrentOS(normalized)) {
            logger.debugf("Skipping native resource '%s' not meant for this '%s' OS", normalized, OS);
            return;
        }

        // If already loaded, nothing to do
        if (_loaded.contains(normalized)) {
            logger.debugf("Native resource '%s' already loaded", normalized);
            return;
        }

        // Acquire per-resource lock to avoid concurrent extraction + load
        final Object lock = _locks.computeIfAbsent(normalized, k -> new Object());
        synchronized (lock) {
            try {
                // Re-check after acquiring lock
                if (_loaded.contains(normalized)) {
                    logger.debugf("Native resource '%s' already loaded (after lock)", normalized);
                    return;
                }

                // actually load from JAR/resources (extract to temp file then System.load)
                try (InputStream in = NativeLoader.class.getResourceAsStream(normalized)) {
                    if (in == null) {
                        throw new IOException(String.format("Native resource '%s' not found on classpath ", normalized));
                    }

                    final File file = new File(normalized);
                    final String filename = file.getName();
                    final File tmp = Files.createTempFile("rfid-native-", "-" + filename).toFile();
                    tmp.deleteOnExit();
                    Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    try {
                        System.load(tmp.getAbsolutePath());
                        logger.infof("Loaded native resource '%s' into '%s'", normalized, tmp.getAbsolutePath());
                    } catch (UnsatisfiedLinkError ule) {
                        // Provide helpful message and include original cause
                        final String msg = String.format("Failed to load native library from extracted file '%s' ", tmp.getAbsolutePath());
                        logger.error(msg, ule);
                        throw new IOException(msg, ule);
                    }
                }

                // mark as loaded
                _loaded.add(normalized);
            } finally {
                // allow the lock object to be GC'd in future (remove mapping)
                _locks.remove(normalized);
            }
        }
    }

    private static boolean shouldLoadForCurrentOS(final String resourcePath) {
        final String name = resourcePath.toLowerCase(Locale.ROOT);

        final int dot = name.lastIndexOf('.');
        if (dot < 0) {
            // no extension -- assume caller expects caller to use platformFileName variant.
            return true;
        }

        final String ext = name.substring(dot + 1);
        switch (ext) {
            case "dll":
                return OS.equals("windows");
            case "dylib":
            case "jnilib":
                return OS.equals("mac");
            case "so":
                // treat .so as non-windows and non-mac
                // return !OS.equals("windows") && !OS.equals("mac");
                return OS.equals("linux");
            default:
                // unknown extension: conservatively allow
                return true;
        }
    }

    private static String normalizeResourcePath(final String resourcePath) {
        Objects.requireNonNull(resourcePath, "Resource path must not be null");
        return resourcePath.startsWith("/") ? resourcePath : ("/" + resourcePath);
    }

    private static String detectOS() {
        final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) return "windows";
        if (osName.contains("mac") || osName.contains("darwin")) return "mac";
        return "linux";
    }

    private static String detectArch() {
        final String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (arch.equals("amd64") || arch.equals("x86_64")) return "x86_64";
        if (arch.equals("aarch64") || arch.equals("arm64")) return "arm64";
        return arch.replaceAll("[^a-z0-9_-]", "");
    }

}
