package com.avrix.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test class for {@link ImageLoader}.
 */
public class ImageLoaderTest {
    /**
     * Tests the {@link ImageLoader#isImageFile(String)} method to ensure it correctly
     * identifies valid image file extensions.
     */
    @Test
    public void testIsImageFile() {
        assertTrue(ImageLoader.isImageFile("png"));
        assertTrue(ImageLoader.isImageFile("jpeg"));
        assertTrue(ImageLoader.isImageFile("jpg"));
        assertTrue(ImageLoader.isImageFile("bmp"));
        assertTrue(ImageLoader.isImageFile("gif"));
        assertTrue(ImageLoader.isImageFile("tiff"));
        assertTrue(ImageLoader.isImageFile("webp"));
        assertFalse(ImageLoader.isImageFile("txt"));
        assertFalse(ImageLoader.isImageFile("pdf"));
        assertFalse(ImageLoader.isImageFile("docx"));
    }
}