package com.avrix.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionCheckerTest {

    @Test
    void normalizeVersion_shouldReturnZero_forNullOrBlank() {
        assertEquals("0", VersionChecker.normalizeVersion(null));
        assertEquals("0", VersionChecker.normalizeVersion(""));
        assertEquals("0", VersionChecker.normalizeVersion("   "));
    }

    @Test
    void normalizeVersion_shouldStripNoiseAndSeparators() {
        assertEquals("41.78.16", VersionChecker.normalizeVersion("v41.78.16-beta"));
        assertEquals("41.78.16", VersionChecker.normalizeVersion("41_078,016"));
        assertEquals("1.2.3", VersionChecker.normalizeVersion("01..002---0003"));
    }

    @Test
    void compareVersions_shouldWork_withDifferentLengths() {
        assertTrue(VersionChecker.compareVersions("1.2", "1.2.0") == 0);
        assertTrue(VersionChecker.compareVersions("1.2.1", "1.2") > 0);
        assertTrue(VersionChecker.compareVersions("1.2", "1.2.1") < 0);
    }

    @Test
    void isVersionCompatible_shouldSupportOperatorsAndSpaces() {
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", ">=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.17", ">= 41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.15", "<=41.78.16"));
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", "==41.78.16"));
        assertFalse(VersionChecker.isVersionCompatible("41.78.15", ">=41.78.16"));
    }

    @Test
    void isVersionCompatible_shouldAssumeEquality_whenNoOperator() {
        assertTrue(VersionChecker.isVersionCompatible("41.78.16", "41.78.16"));
        assertFalse(VersionChecker.isVersionCompatible("41.78.17", "41.78.16"));
    }

    @Test
    void isVersionCompatible_shouldRejectBlankCondition() {
        assertThrows(IllegalArgumentException.class,
                () -> VersionChecker.isVersionCompatible("1.0.0", "   "));
    }

    @Test
    void isVersionCompatible_shouldRejectConditionWithoutDigits() {
        assertThrows(IllegalArgumentException.class,
                () -> VersionChecker.isVersionCompatible("1.0.0", ">=beta"));
    }
}