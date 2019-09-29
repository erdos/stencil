package io.github.erdos.stencil.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * File handling utilities.
 * Some methods may be called from Clojure core.
 */
@SuppressWarnings("WeakerAccess")
public final class FileHelper {

    private final static File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

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
        return new File(TEMP_DIRECTORY, prefix + UUID.randomUUID().toString() + suffix);
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

    /**
     * Recursively marks a directory or a file for deletion on exit.
     *
     * @param file to delete, not null
     * @throws NullPointerException on null or invalid file
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public static void forceDeleteOnExit(final File file) {
        file.deleteOnExit();
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                forceDeleteOnExit(child);
            }
        }
    }

    /**
     * Returns a string representation of path with unix separators ("/") instead of the
     * system-dependent separators (which is backslash on windows.)
     *
     * @param path not null path object
     * @return string of path with slash separators
     * @throws IllegalArgumentException if path is null
     */
    public static String toUnixSeparatedString(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null!");
        } else {
            final String separator = FileSystems.getDefault().getSeparator();
            if (separator.equals("/")) {
                // on unix systems
                return path.toString();
            } else {
                // on windows systems we replace backslash with slashes
                return path.toString().replaceAll(Pattern.quote(separator), "/");
            }
        }
    }
}
