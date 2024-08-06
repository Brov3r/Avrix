package com.avrix.ui;

import com.avrix.Launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;

/**
 * A set of tools for working with fonts in NanoVG
 */
public class NanoFont {
    /**
     * Loading custom default fonts
     */
    public static void loadDefaultFonts() {
        try {
            File coreJarFile = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            createFont("Montserrat-Regular", coreJarFile.getPath(), "media/fonts/Montserrat-Regular.ttf");
            createFont("Arial-Regular", coreJarFile.getPath(), "media/fonts/Arial-Regular.ttf");
            createFont("Roboto-Regular", coreJarFile.getPath(), "media/fonts/Roboto-Regular.ttf");
            createFont("FontAwesome", coreJarFile.getPath(), "media/fonts/FontAwesome.ttf");
        } catch (Exception e) {
            System.out.println("[!] Failed to load custom fonts: " + e.getMessage());
        }
    }

    /**
     * Creates and loads a font into NanoVG with the specified name from a font file inside a JAR.
     *
     * @param fontName         the name to assign to the font.
     * @param jarFilePath      the path to the JAR file containing the font.
     * @param internalFilePath the path to the font file inside the JAR.
     * @throws IOException if an I/O error occurs while reading the font file.
     */
    public static void createFont(String fontName, String jarFilePath, String internalFilePath) throws IOException {
        URL jarUrl = new URL("jar:file:" + jarFilePath + "!/" + internalFilePath);
        try (InputStream inputStream = jarUrl.openStream()) {
            Path tempFontFile = Files.createTempFile(fontName + "_temp-font", ".ttf");
            Files.copy(inputStream, tempFontFile, StandardCopyOption.REPLACE_EXISTING);
            createFont(fontName, tempFontFile);

            Files.delete(tempFontFile);
        } catch (IOException e) {
            System.out.printf("[!] File '%s' not found inside JAR file '%s'!%n", internalFilePath, jarFilePath);
            throw e;
        }
    }

    /**
     * Creates and loads a font into NanoVG with the specified name from the given path.
     *
     * @param fontName the name to assign to the font.
     * @param fontPath the path to the font file (e.g., a TrueType font file).
     */
    public static void createFont(String fontName, Path fontPath) {
        if (WidgetManager.getContext() == null) {
            System.out.println("[!] Font creation must occur after the WidgetManager is initialized! Use the `OnWidgetManagerInitializedEvent`!");
            return;
        }

        int fontId = nvgCreateFont(WidgetManager.getContext().get(), fontName, fontPath.toString());
        if (fontId == -1) {
            System.out.printf("[!] Failed to load font '%s' at path: '%s'%n", fontName, fontPath);
        }
    }
}