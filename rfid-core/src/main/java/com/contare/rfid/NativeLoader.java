package com.contare.rfid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Helper to load a platform native library packaged inside the module JAR.
 * <p>
 * Place native libraries inside:
 * src/main/resources/native/<os>-<arch>/<libfile>
 * <p>
 * Usage:
 * NativeLoader.loadPlatformLibrary("chainway");
 * <p>
 * This extracts the resource to a temporary file and calls System.load(tempPath).
 */
public final class NativeLoader {

    private NativeLoader() {
        // ignore
    }

    public static void loadPlatformLibrary(final String baseName) throws IOException {
        final String os = detectOS();
        final String arch = detectArch();
        final String fileName = platformFileName(baseName, os);
        final String resourcePath = "/native/" + os + "-" + arch + "/" + fileName;
        loadLibraryFromJar(resourcePath);
    }

    public static void loadLibraryFromJar(final String resourcePath) throws IOException {
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Native resource not found on classpath: " + resourcePath);
            }
            final String fileName = new File(resourcePath).getName();
            final File temp = Files.createTempFile("rfid-native-", "-" + fileName).toFile();
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.getAbsolutePath());
        }
    }

    private static String platformFileName(final String baseName, final String os) {
        switch (os) {
            case "windows":
                return baseName + ".dll";
            case "mac":
                return "lib" + baseName + ".dylib";
            default:
                return "lib" + baseName + ".so";
        }
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
