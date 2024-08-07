package com.avrix.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the VersionChecker class.
 */
public class VersionCheckerTest {
    /**
     * Tests the normalizeVersion method with valid version strings.
     * Ensures that the method correctly normalizes various valid versions.
     */
    @Test
    public void testValidVersions() {
        assertEquals("1.0.0", VersionChecker.normalizeVersion("v1.0.0"));
        assertEquals("2.2.2", VersionChecker.normalizeVersion("v2.2_2-ad"));
        assertEquals("3.1.4", VersionChecker.normalizeVersion("v3.1.4!"));
        assertEquals("4.5.6", VersionChecker.normalizeVersion("4-5-6"));
        assertEquals("7.8.9", VersionChecker.normalizeVersion("7_8_9"));
        assertEquals("10.11.12", VersionChecker.normalizeVersion("v10.11.12-"));
    }

    /**
     * Tests the normalizeVersion method with invalid version strings.
     * Ensures that the method correctly removes invalid characters and normalizes the versions.
     */
    @Test
    public void testInvalidVersions() {
        assertEquals("1.0.0", VersionChecker.normalizeVersion("1.0.0extra"));
        assertEquals("2.3", VersionChecker.normalizeVersion("2.3extra!"));
        assertEquals("4.5.6", VersionChecker.normalizeVersion("4.5.6extra@"));
        assertEquals("10.11", VersionChecker.normalizeVersion("10.11!extra"));
        assertEquals("9", VersionChecker.normalizeVersion("!9"));
    }

    /**
     * Tests the normalizeVersion method with edge cases.
     * Ensures that the method correctly handles empty strings, null values, and versions with multiple delimiters.
     */
    @Test
    public void testEdgeCases() {
        assertEquals("", VersionChecker.normalizeVersion(""));
        assertEquals("", VersionChecker.normalizeVersion(null));
        assertEquals("1.2.3", VersionChecker.normalizeVersion("1.2.3."));
        assertEquals("4.5.6", VersionChecker.normalizeVersion("4.5.6@"));
        assertEquals("0.0.0", VersionChecker.normalizeVersion("0000--00..00"));
        assertEquals("1.20.3000", VersionChecker.normalizeVersion("0001--020..0003000"));
    }

    /**
     * Checking two versions
     */
    @Test
    public void testCompareVersions() {
        assertEquals(0, VersionChecker.compareVersions("41.78.16", "41.78.16"));
        assertTrue(VersionChecker.compareVersions("41.78.17", "41.78.16") > 0);
        assertTrue(VersionChecker.compareVersions("41.78.15", "41.78.16") < 0);
        assertEquals(0, VersionChecker.compareVersions("41.78.16", "41.78.16.0"));
        assertTrue(VersionChecker.compareVersions("41.78.16.1", "41.78.16") > 0);
        assertTrue(VersionChecker.compareVersions("41.78.15", "41.78.15.1") < 0);
    }

    /**
     * Tests compatibility for versions with different formats.
     */
    @Test
    public void testDifferentFormats() {
        assertTrue(VersionChecker.isVersionCompatible("41f.78s.16a-Beta", "=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16-Beta", "=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16-Beta", "=41.78.16-Beta"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16_RS", "41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16.1", ">41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41-78-16", "41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41,78,16", "41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41,78-16", "41.78.16"));
    }

    /**
     * Tests compatibility for exact version matches.
     */
    @Test
    public void testExactMatches() {
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", "=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", "41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.2", "0.1.2"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.2", "=0.1.2"));
        assertTrue(VersionChecker.isVersionCompatible("0.1", "0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.1", "=0.1"));
        assertTrue(VersionChecker.isVersionCompatible("1", "1"));
        assertTrue(VersionChecker.isVersionCompatible("1", "=1"));
    }

    /**
     * Tests compatibility for greater than and greater than or equal to comparisons.
     */
    @Test
    public void testGreaterComparisons() {
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", ">=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.17", ">41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.3", ">0.1.2"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.2", ">=0.1.2"));
        assertTrue(VersionChecker.isVersionCompatible("0.2", ">0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.1", ">0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.0.1", ">0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.1", ">=0.1"));
        assertTrue(VersionChecker.isVersionCompatible("2", ">1"));
        assertTrue(VersionChecker.isVersionCompatible("1", ">=1"));
    }

    /**
     * Tests compatibility for less than and less than or equal to comparisons.
     */
    @Test
    public void testLesserComparisons() {
        assertTrue(VersionChecker.isVersionCompatible("41.78.15", "<41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", "<=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.1", "<0.1.2"));
        assertTrue(VersionChecker.isVersionCompatible("0.1.2", "<=0.1.2"));
        assertTrue(VersionChecker.isVersionCompatible("0.0", "<0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.1", "<=0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.0.9", "<0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0.0.0.1", "<0.1"));
        assertTrue(VersionChecker.isVersionCompatible("0", "<1"));
        assertTrue(VersionChecker.isVersionCompatible("1", "<=1"));
    }
}