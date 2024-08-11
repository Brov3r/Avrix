package com.avrix.ui;

import com.avrix.plugin.ResourceManager;
import org.lwjgl.nanovg.NanoVG;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;

    /**
     * Loads an image from a JAR file.
     *
     * @param jarFilePath      Path to the JAR file
     * @param internalFilePath Path to the image inside the JAR
     * @return the image ID, or -1 if loading failed
     */
    public static int loadImage(String jarFilePath, String internalFilePath) {
        String jarPath = "jar:file:" + jarFilePath + "!/" + internalFilePath;
        return loadImageInternal(() -> new URL(jarPath), jarPath);
    }

    /**
     * Loads an image from a file path and returns its ID. The image is cached for future use.
     *
     * @param path the path to the image file
     * @return the image ID, or -1 if loading failed
     */
    public static int loadImage(Path path) {
        return loadImageInternal(() -> path.toUri().toURL(), path.toString());
    }

    /**
     * Loads an image from a URL and returns its ID. The image is cached for future use.
     *
     * @param url the URL of the image
     * @return the image ID, or -1 if loading failed
     */
    public static int loadImage(URL url) {
        return loadImageInternal(() -> url, url.getPath());
    }

    /**
     * Loads an image from a URL string and returns its ID. The image is cached for future use.
     *
     * @param urlString the URL string of the image
     * @return the image ID, or -1 if loading failed
     */
    public static int loadImage(String urlString) {
        return loadImageInternal(() -> new URL(urlString), urlString);
    }

    /**
     * Internal method to load an image using a {@link ResourceLoader} and cache the result.
     *
     * @param loader     a {@link ResourceLoader} that provides the image's URL
     * @param identifier a unique identifier for the image, used for caching
     * @return the image ID, or -1 if loading failed
     */
    private static int loadImageInternal(ResourceLoader loader, String identifier) {
        if (WidgetManager.getContext() == null) {
            return -1;
        }

        String cacheFileName = ResourceManager.encodeFileName(identifier);
        String fileExtension = ResourceManager.getFileExtension(identifier);
        Path cacheFilePath = imagesCachePath.resolve(cacheFileName + "." + fileExtension);

        if (imagesCacheMap.containsKey(cacheFileName)) {
            return imagesCacheMap.get(cacheFileName);
        }

        if (Files.exists(cacheFilePath)) {
            return loadFromCache(cacheFilePath, cacheFileName);
        }

        try {
            URL resourceUrl = loader.load();
            return loadResource(resourceUrl.openStream(), identifier, cacheFileName);
        } catch (IOException e) {
            System.out.printf("[!] Failed to load image! Error with resource '%s': %s!%n", identifier, e.getMessage());
            imagesCacheMap.put(cacheFileName, -1);
            return -1;
        }
    }

    /**
     * Loads an image from the cache and returns its ID.
     *
     * @param cacheFilePath the path to the cached image file
     * @param cacheFileName the cached file name used as the key in the cache map
     * @return the image ID, or -1 if loading from the cache failed
     */
    private static int loadFromCache(Path cacheFilePath, String cacheFileName) {
        try {
            int imageID = NanoVG.nvgCreateImage(WidgetManager.getContext().get(), cacheFilePath.toString(), NanoVG.NVG_IMAGE_NEAREST);
            imagesCacheMap.put(cacheFileName, imageID);
            System.out.printf("[#] Cache image '%s' loaded with ID %d!%n", cacheFilePath, imageID);
            return imageID;
        } catch (Exception e) {
            System.out.printf("[!] Failed to load image from cache! Error with file '%s': %s!%n", cacheFilePath, e.getMessage());
            imagesCacheMap.put(cacheFileName, -1);
            return -1;
        }
    }

    /**
     * Loads an image from an input stream, caches it, and returns its ID.
     *
     * @param inputStream   the input stream of the image
     * @param fileName      the name of the image file
     * @param cacheFileName the cached file name used as the key in the cache map
     * @return the image ID, or -1 if loading failed
     */
    private static int loadResource(InputStream inputStream, String fileName, String cacheFileName) {
        if (!isImageFile(ResourceManager.getFileExtension(fileName))) {
            System.out.printf("[!] Failed to load image! File '%s' is not a supported image type!%n", fileName);
            imagesCacheMap.put(cacheFileName, -1);
            return -1;
        }

        String fileExtension = ResourceManager.getFileExtension(fileName);
        Path cacheFilePath = imagesCachePath.resolve(cacheFileName + "." + fileExtension);

        try (InputStream in = inputStream) {
            // Load the image into memory
            BufferedImage originalImage = ImageIO.read(in);
            if (originalImage == null) {
                System.out.printf("[!] Failed to load image! File '%s' could not be read as an image!%n", fileName);
                imagesCacheMap.put(cacheFileName, -1);
                return -1;
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // Resize image if it exceeds the maximum allowed resolution
            if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                double widthRatio = (double) MAX_WIDTH / width;
                double heightRatio = (double) MAX_HEIGHT / height;
                double scalingFactor = Math.min(widthRatio, heightRatio);

                int newWidth = (int) (width * scalingFactor);
                int newHeight = (int) (height * scalingFactor);

                Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());

                Graphics2D g2d = resizedImage.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();

                // Save the resized image to the cache file path
                Files.createDirectories(cacheFilePath.getParent());
                ImageIO.write(resizedImage, fileExtension, cacheFilePath.toFile());
            } else {
                // Save the original image to the cache file path
                Files.createDirectories(cacheFilePath.getParent());
                ImageIO.write(originalImage, fileExtension, cacheFilePath.toFile());
            }

            // Create NanoVG image from the cached file
            int imageID = NanoVG.nvgCreateImage(WidgetManager.getContext().get(), cacheFilePath.toString(), NanoVG.NVG_IMAGE_NEAREST);
            imagesCacheMap.put(cacheFileName, imageID);
            System.out.printf("[#] Image '%s' loaded and cached with ID %d!%n", fileName, imageID);
            return imageID;
        } catch (Exception e) {
            System.out.printf("[!] Failed to load image! Error with file path '%s': %s!%n", cacheFilePath, e.getMessage());
            imagesCacheMap.put(cacheFileName, -1);
            return -1;
        }
    }

    /**
     * Functional interface to handle the resource loading
     */
    @FunctionalInterface
    private interface ResourceLoader {
        URL load() throws IOException;
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