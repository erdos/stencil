package io.github.erdos.stencil.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.github.erdos.stencil.impl.FileHelper.forceMkdir;

/**
 * Various helpers for handling ZIP files.
 */
public final class ZipHelper {

    private ZipHelper() {}

    /**
     * Unzips contents of a zip file under the target directory. Closes stream.
     * The unzipped files keep their relative paths from the zip file.
     * That is files from the root of the zip file will be put directly under target directory, etc.
     *
     * @param zipFileStream        input stream of a ZIP file
     * @param unzipTargetDirectory a directory where zip contents are put
     * @throws IllegalArgumentException when any param is null.
     * @throws IllegalStateException    when target file already exists.
     * @throws IOException              on file system error
     */
    public static void unzipStreamIntoDirectory(InputStream zipFileStream, File unzipTargetDirectory) throws IOException {
        if (zipFileStream == null) {
            throw new IllegalArgumentException("zip file stream is null!");
        } else if (unzipTargetDirectory == null) {
            throw new IllegalArgumentException("target directory is null!");
        } else if (unzipTargetDirectory.exists()) {
            throw new IllegalStateException("unzip target dir already exists: " + unzipTargetDirectory);
        }

        forceMkdir(unzipTargetDirectory);

        byte[] buffer = new byte[1024];
        int len;

        try (ZipInputStream zis = new ZipInputStream(zipFileStream)) {
            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                File newFile = new File(unzipTargetDirectory, zipEntry.getName());
                forceMkdir(newFile.getParentFile());
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
            zis.closeEntry();
        }
        zipFileStream.close();
    }
}
