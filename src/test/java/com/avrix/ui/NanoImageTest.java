package com.avrix.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test class for {@link NanoImage}.
 */
public class NanoImageTest {
    /**
     * Tests the {@link NanoImage#isImageFile(String)} method to ensure it correctly
     * identifies valid image file extensions.
     */
    @Test
    public void testIsImageFile() {
        assertTrue(NanoImage.isImageFile("png"));
        assertTrue(NanoImage.isImageFile("jpeg"));
        assertTrue(NanoImage.isImageFile("jpg"));
        assertTrue(NanoImage.isImageFile("bmp"));
        assertTrue(NanoImage.isImageFile("gif"));
        assertTrue(NanoImage.isImageFile("tiff"));
        assertTrue(NanoImage.isImageFile("webp"));
        assertFalse(NanoImage.isImageFile("txt"));
        assertFalse(NanoImage.isImageFile("pdf"));
        assertFalse(NanoImage.isImageFile("docx"));
    }
}