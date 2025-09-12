package io.github.erdos.stencil.impl;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static java.lang.System.getProperty;

/**
 * File handling utilities.
 * Some methods may be called from Clojure core.
 */
@SuppressWarnings("WeakerAccess")
public final class FileHelper {

    private final static File TEMP_DIRECTORY = new File(requireNonNullElse(getProperty("stencil.tmpdir"), getProperty("java.io.tmpdir")));

    private FileHelper() {}

    public static String extension(File f) {
        return extension(f.getName());
    }

    public static String extension(String filename) {
        String[] parts = filename.split("\\.");
        return parts[parts.length - 1].trim().toLowerCase();
    }

    /**
     * Returns file name without extension part.
     *
     * @return simple file name without extension.
     * @throws NullPointerException the input is null
     */
    public static String removeExtension(File f) {
        String fileName = f.getName();
        if (fileName.contains(".")) {
            int loc = fileName.lastIndexOf('.');
            return fileName.substring(0, loc);
        } else {
            return fileName;
        }
    }

    /**
     * Creates a temporary file that is guaranteed not to exist on file system.
     *
     * @param prefix file name starts with this file
     * @param suffix file name ends with this file
     * @return a new file object pointing to a non-existing file in temp directory.
     */
    public static File createNonexistentTempFile(String prefix, String suffix) {
        return createNonexistentTempFile(null, prefix, suffix);
    }

    public static File createNonexistentTempFile(File parent, String prefix, String suffix) {
        return new File(requireNonNullElse(parent, TEMP_DIRECTORY), prefix + UUID.randomUUID() + suffix);
    }

    /**
     * Creates a directory. Recursively creates parent directories too.
     *
     * @param directory not null dir to create
     * @throws IOException              on IO error
     * @throws IllegalArgumentException when input is null or already exists
     */
    public static void forceMkdir(final File directory) throws IOException {
        if (directory == null)
            throw new IllegalArgumentException("Missing directory for forceMkdir");
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException("File exists and not a directory: " + directory);
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory()) {
                    throw new IOException("Unable to create directory " + directory);
                }
            }
        }
    }

    /**
     * Recursively deletes a directory or a file.
     *
     * @param file to delete, not null
     * @throws NullPointerException on null or invalid file
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public static void forceDelete(final File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                forceDelete(child);
            }
        }
        file.delete();
    }

    private static <T> T requireNonNullElse(T first, T second) {
        return first == null ? second : first;
    }
}
