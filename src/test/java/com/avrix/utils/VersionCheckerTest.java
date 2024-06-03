package com.avrix.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the VersionChecker class.
 */
public class VersionCheckerTest {

    /**
     * Tests compatibility for versions with different formats.
     */
    @Test
    public void testDifferentFormats() {
        assertFalse(VersionChecker.isVersionCompatible("41f.78s.16a-Beta", "=41.78.16"));
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