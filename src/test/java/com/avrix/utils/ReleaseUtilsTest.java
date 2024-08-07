package com.avrix.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests to check the latest release version
 */
public class ReleaseUtilsTest {
    /**
     * Tests the getLatestReleaseVersion method.
     * Ensures that the method retrieves the latest release version and normalizes it.
     *
     * @throws ExecutionException   if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    public void getVersionReleaseTest() throws ExecutionException, InterruptedException {
        ReleaseUtils.checkLatestVersion();

        CompletableFuture<String> releaseVersionFuture = ReleaseUtils.getLatestReleaseVersion();
        String releaseVersion = VersionChecker.normalizeVersion(releaseVersionFuture.get());

        System.out.printf("[#] Current Avrix version %s, latest %s%n", Constants.AVRIX_VERSION, releaseVersion);

        assertNotNull(releaseVersion);
    }
}