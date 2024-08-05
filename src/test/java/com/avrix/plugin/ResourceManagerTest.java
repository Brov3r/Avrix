package com.avrix.plugin;

import com.avrix.utils.Constants;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link ResourceManager} class.
 */
public class ResourceManagerTest {
    /**
     * Tests the {@link ResourceManager#getFolderSize(File)} method to ensure it correctly calculates the size of a folder.
     *
     * @throws FileNotFoundException if the "yaml" folder is not found in the resources.
     * @throws URISyntaxException    if the URI of the folder cannot be constructed.
     */
    @Test
    public void testFolderSize() throws FileNotFoundException, URISyntaxException {
        URL resourceUrl = ResourceManagerTest.class.getClassLoader().getResource("yaml");
        if (resourceUrl == null) {
            throw new FileNotFoundException("[!] Yaml folder not found in resources");
        }

        File folder = new File(resourceUrl.toURI());
        long folderSize = ResourceManager.getFolderSize(folder) / (1024 * 1024); // mb

        assertTrue(folderSize < Constants.MAX_CACHE_SIZE);
    }

    /**
     * Tests the {@link ResourceManager#getFileExtension(String)} method to ensure it correctly extracts file extensions.
     */
    @Test
    public void testGetFileExtension() {
        assertEquals("png", ResourceManager.getFileExtension("image.png"));
        assertEquals("jpeg", ResourceManager.getFileExtension("photo.jpeg"));
        assertEquals("jpg", ResourceManager.getFileExtension("picture.jpg"));
        assertEquals("bmp", ResourceManager.getFileExtension("bitmap.bmp"));
        assertEquals("gif", ResourceManager.getFileExtension("animation.gif"));
        assertEquals("tiff", ResourceManager.getFileExtension("scan.tiff"));
        assertEquals("webp", ResourceManager.getFileExtension("webimage.webp"));
        assertEquals("", ResourceManager.getFileExtension("nofileextension"));
        assertEquals("", ResourceManager.getFileExtension(".hiddenfile"));
    }

    /**
     * Tests the {@link ResourceManager#removeFileExtension(String)} method to ensure it correctly removes file extensions.
     */
    @Test
    public void testRemoveFileExtension() {
        assertEquals("image", ResourceManager.removeFileExtension("image.png"));
        assertEquals("photo", ResourceManager.removeFileExtension("photo.jpeg"));
        assertEquals("picture", ResourceManager.removeFileExtension("picture.jpg"));
        assertEquals("bitmap", ResourceManager.removeFileExtension("bitmap.bmp"));
        assertEquals("animation", ResourceManager.removeFileExtension("animation.gif"));
        assertEquals("scan", ResourceManager.removeFileExtension("scan.tiff"));
        assertEquals("webimage", ResourceManager.removeFileExtension("webimage.webp"));
        assertEquals("nofileextension", ResourceManager.removeFileExtension("nofileextension"));
        assertEquals(".hiddenfile", ResourceManager.removeFileExtension(".hiddenfile"));
    }

    /**
     * Tests the {@link ResourceManager#clearCache()} method to ensure it correctly clears the cache when the cache size exceeds the limit.
     *
     * @throws IOException if an I/O error occurs while creating files or directories.
     */
    @Test
    public void testClearCache() throws IOException {
        Path tempDir = Files.createTempDirectory("tempCache");

        // Adding files to a folder to exceed the limit
        for (int i = 0; i < 10; i++) {
            File tempFile = Files.createTempFile(tempDir, "tempFile", ".tmp").toFile();
            try (var fos = new java.io.FileOutputStream(tempFile)) {
                // We write 26 MB of data to each file
                byte[] data = new byte[1024 * 1024 * 26];
                fos.write(data);
            }
        }

        // Set the cache path to a temporary folder
        ResourceManager.cachePath = tempDir;

        // Checking that the folder size exceeds the limit
        long folderSizeBytes = ResourceManager.getFolderSize(tempDir.toFile());
        long folderSizeMB = folderSizeBytes / (1024 * 1024);
        assertTrue(folderSizeMB > Constants.MAX_CACHE_SIZE);

        // Call the cache clearing method
        ResourceManager.clearCache();

        // Checking that the folder has been cleared
        assertEquals(0, Objects.requireNonNull(tempDir.toFile().list()).length);

        // Delete the temporary folder after the test
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        }
    }
}