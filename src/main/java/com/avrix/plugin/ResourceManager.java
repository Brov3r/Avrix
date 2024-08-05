package com.avrix.plugin;

import com.avrix.utils.Constants;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Manages resources such as images, including caching and encoding utilities.
 */
public class ResourceManager {
    /**
     * The path to the cache directory.
     */
    public static Path cachePath = Paths.get(Constants.CACHE_DIR_NAME);

    /**
     * Initializes the resource manager by setting up the cache directory and loading resources.
     */
    public static void init() {
        File cacheFolder = cachePath.toFile();

        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            System.out.println("[!] Failed to create the cache folder.");
            return;
        }

        clearCache();
    }

    /**
     * Encodes a file name to a Base64 string.
     *
     * @param fileName The name of the file to encode.
     * @return The Base64 encoded string representation of the file name.
     */
    public static String encodeFileName(String fileName) {
        byte[] bytes = fileName.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Retrieves the file extension from a file name.
     *
     * @param fileName The name of the file from which to extract the extension.
     * @return The file extension in lowercase, or an empty string if there is no extension.
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Removes the file extension from a file name.
     *
     * @param fileName The name of the file from which to remove the extension.
     * @return The file name without the extension, or the original name if no extension exists.
     */
    public static String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }

    /**
     * Clears the cache if it exceeds the maximum allowed size.
     */
    public static void clearCache() {
        int cacheSize = Constants.MAX_CACHE_SIZE;

        long folderSizeBytes = getFolderSize(cachePath.toFile());
        long folderSizeMB = folderSizeBytes / (1024 * 1024); // convert bytes to megabytes

        if (folderSizeMB > cacheSize) {
            System.out.println("[?] Cache size exceeds limit. Clearing cache...");
            deleteFolderContents(cachePath.toFile());
        }
    }

    /**
     * Recursively calculates the size of a folder in bytes.
     *
     * @param folder The folder whose size is to be calculated.
     * @return The total size of the folder in bytes.
     */
    public static long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += getFolderSize(file);
                }
            }
        }
        return length;
    }

    /**
     * Deletes all contents of a folder, including subfolders and files.
     *
     * @param folder The folder whose contents are to be deleted.
     */
    public static void deleteFolderContents(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                } else {
                    deleteFolderContents(file);
                    file.delete();
                }
            }
        }
    }
}