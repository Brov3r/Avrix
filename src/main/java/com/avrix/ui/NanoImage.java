package com.avrix.ui;

import com.avrix.plugin.ResourceManager;
import org.lwjgl.nanovg.NanoVG;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for loading images and caching them.
 */
public class NanoImage {
    /**
     * Map to store image identifiers associated with their names.
     */
    public static Map<String, Integer> imagesCacheMap = new ConcurrentHashMap<>();

    /**
     * Path to the image cache directory.
     */
    public static Path imagesCachePath = ResourceManager.cachePath.resolve("images");

    /**
     * Loads images from the cache directory into the NVG context.
     * Creates the cache directory if it does not exist.
     *
     * @param context The NVG context used to load textures.
     */
    public static void loadCacheImages(NanoContext context) {
        File cacheFolder = imagesCachePath.toFile();

        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            System.out.println("[!] Failed to create the cache folder.");
            return;
        }

        File[] files = cacheFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImageFile(ResourceManager.getFileExtension(file.getName()))) {
                    String fileNameWithoutExtension = ResourceManager.removeFileExtension(file.getName());
                    try {
                        int imageID = NanoVG.nvgCreateImage(context.get(), file.getAbsolutePath(), NanoVG.NVG_IMAGE_NEAREST);
                        imagesCacheMap.put(fileNameWithoutExtension, imageID);
                    } catch (Exception e) {
                        System.out.printf("[!] Failed to load image '%s': %s%n", file.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Loads an image from a JAR file.
     *
     * @param jarFilePath      Path to the JAR file.
     * @param internalFilePath Path to the image inside the JAR.
     * @return The image identifier or -1 if the image could not be loaded.
     */
    public static int loadImage(String jarFilePath, String internalFilePath) {
        try {
            URL jarUrl = new URL("jar:file:" + jarFilePath + "!/" + internalFilePath);
            return loadResourceFromStream(jarUrl.openStream(), internalFilePath);
        } catch (IOException e) {
            System.out.printf("[!] Failed load image! Invalid JAR file path: '%s'!%n", jarFilePath);
            return -1;
        }
    }

    /**
     * Loads an image from a local file.
     *
     * @param path Path to the local image file.
     * @return The image identifier or -1 if the image could not be loaded.
     */
    public static int loadImage(Path path) {
        try {
            if (!Files.exists(path)) {
                System.out.printf("[!] Failed load image! File '%s' does not exist!%n", path);
                return -1;
            }
            String fileName = path.getFileName().toString();
            if (!isImageFile(ResourceManager.getFileExtension(fileName))) {
                System.out.printf("[!] Failed load image! File '%s' is not a supported image type!%n", fileName);
                return -1;
            }
            return loadResourceFromStream(Files.newInputStream(path), fileName);
        } catch (IOException e) {
            System.out.printf("[!] Failed load image! Error with file at '%s': %s!%n", path, e.getMessage());
            return -1;
        }
    }

    /**
     * Loads an image from a URL.
     *
     * @param url The URL string to load the image from.
     * @return The image identifier or -1 if the image could not be loaded.
     */
    public static int loadImage(URL url) {
        if (WidgetManager.getContext() == null) {
            System.out.println("[!] The image could not be loaded because the NanoVG context was not defined! Use this method after initializing the WidgetManager!");
            return -1;
        }

        if (url == null) {
            System.out.println("[!] Failed load image! Invalid URL!");
            return -1;
        }

        String fileName = Paths.get(url.getPath()).toString();
        String fileExtension = ResourceManager.getFileExtension(fileName);
        String cacheFileName = ResourceManager.encodeFileName(fileName);

        if (imagesCacheMap.containsKey(cacheFileName)) {
            return imagesCacheMap.get(cacheFileName);
        }

        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                System.out.printf("[!] Failed load image! Unable to decode image from URL: '%s'!%n", url);
                return -1;
            }

            Path cacheFilePath = imagesCachePath.resolve(cacheFileName + "." + fileExtension);
            File cacheFile = cacheFilePath.toFile();

            if (!cacheFile.exists()) {
                Files.createDirectories(cacheFilePath.getParent());
                ImageIO.write(image, fileExtension, cacheFile);
            }

            int imageID = NanoVG.nvgCreateImage(WidgetManager.getContext().get(), cacheFile.getAbsolutePath(), NanoVG.NVG_IMAGE_NEAREST);
            imagesCacheMap.put(cacheFileName, imageID);
            System.out.printf("[#] Image '%s' loaded and cached with ID %d!%n", fileName, imageID);
            return imageID;
        } catch (IOException e) {
            System.out.printf("[!] Failed load image! Invalid URL or I/O Error: '%s'!%n", e.getMessage());
        }
        return -1;
    }

    /**
     * Loads an image from a URL.
     *
     * @param urlString The URL string to load the image from.
     * @return The image identifier or -1 if the image could not be loaded.
     */
    public static int loadImage(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
            return loadImage(url);
        } catch (Exception e) {
            System.out.printf("[!] Failed load image! Invalid URL or I/O Error: '%s'!%n", e.getMessage());
            return -1;
        }
    }

    /**
     * Loads an image from an input stream and saves it to cache.
     *
     * @param inputStream The input stream containing the image data.
     * @param fileName    The name of the image file.
     * @return The image identifier or -1 if the image could not be loaded.
     */
    private static int loadResourceFromStream(InputStream inputStream, String fileName) {
        if (WidgetManager.getContext() == null) {
            System.out.println("[!] The image could not be loaded because the NanoVG context was not defined! Use this method after initializing the WidgetManager!");
            return -1;
        }

        String cacheFileName = ResourceManager.encodeFileName(fileName);
        String fileExtension = ResourceManager.getFileExtension(fileName);
        Path cacheFilePath = imagesCachePath.resolve(cacheFileName + "." + fileExtension);

        if (imagesCacheMap.containsKey(cacheFileName)) {
            return imagesCacheMap.get(cacheFileName);
        }

        try (InputStream in = inputStream) {
            Files.createDirectories(cacheFilePath.getParent());
            Files.copy(in, cacheFilePath, StandardCopyOption.REPLACE_EXISTING);

            int imageID = NanoVG.nvgCreateImage(WidgetManager.getContext().get(), cacheFilePath.toString(), NanoVG.NVG_IMAGE_NEAREST);
            imagesCacheMap.put(cacheFileName, imageID);
            System.out.printf("[#] Image '%s' loaded and cached with ID %d!%n", fileName, imageID);
            return imageID;
        } catch (Exception e) {
            System.out.printf("[!] Failed to load image! Error with file path '%s': %s!%n", cacheFilePath, e.getMessage());
        }
        return -1;
    }

    /**
     * Checks if the given file extension is a supported image type.
     *
     * @param fileExtension The file extension to check.
     * @return true if the file extension is a supported image type; false otherwise.
     */
    public static boolean isImageFile(String fileExtension) {
        return switch (fileExtension) {
            case "png", "jpg", "jpeg", "bmp", "gif", "tiff", "webp" -> true;
            default -> false;
        };
    }
}