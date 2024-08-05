package com.avrix.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test class for {@link NVGImage}.
 */
public class NVGImageTest {
    /**
     * Tests the {@link NVGImage#isImageFile(String)} method to ensure it correctly
     * identifies valid image file extensions.
     */
    @Test
    public void testIsImageFile() {
        assertTrue(NVGImage.isImageFile("png"));
        assertTrue(NVGImage.isImageFile("jpeg"));
        assertTrue(NVGImage.isImageFile("jpg"));
        assertTrue(NVGImage.isImageFile("bmp"));
        assertTrue(NVGImage.isImageFile("gif"));
        assertTrue(NVGImage.isImageFile("tiff"));
        assertTrue(NVGImage.isImageFile("webp"));
        assertFalse(NVGImage.isImageFile("txt"));
        assertFalse(NVGImage.isImageFile("pdf"));
        assertFalse(NVGImage.isImageFile("docx"));
    }
}