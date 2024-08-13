package com.avrix.utils;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for checking the latest release version of Avrix from GitHub.
 */
public class ReleaseUtils {

    /**
     * URL for the GitHub API to get the latest release information.
     */
    public static final String API_URL = "https://api.github.com/repos/" + Constants.REPO_OWNER + "/" + Constants.REPO_NAME + "/releases/latest";

    /**
     * URL latest release pages
     */
    public static final String RELEASE_URL = "https://github.com/" + Constants.REPO_OWNER + "/" + Constants.REPO_NAME + "/releases/latest";

    /**
     * Checks the latest version of Avrix available on GitHub.
     * If a newer version is found, prints a message to the console.
     */
    public static void checkLatestVersion() {
        try {
            CompletableFuture<String> releaseVersionFuture = ReleaseUtils.getLatestReleaseVersion();
            String releaseVersion = VersionChecker.normalizeVersion(releaseVersionFuture.get());

            if (VersionChecker.compareVersions(Constants.AVRIX_VERSION, releaseVersion) < 0) {
                System.out.println("[#] A new Avrix update has been released: " + releaseVersion);
                System.out.println("[#] You can download it from the link: " + RELEASE_URL);
            }
        } catch (Exception e) {
            System.out.println("[?] Failed to retrieve the latest Avrix version due to a server or network issue. Please check your internet connection or try again later.");
        }
    }

    /**
     * Gets the latest release version of Avrix from GitHub.
     *
     * @return A CompletableFuture containing the latest release version as a String.
     */
    public static CompletableFuture<String> getLatestReleaseVersion() {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(ReleaseUtils::parseReleaseVersion);
    }

    /**
     * Parses the release version from the JSON response body.
     *
     * @param responseBody The response body from the GitHub API.
     * @return The release version as a String.
     */
    private static String parseReleaseVersion(String responseBody) {
        JSONObject releaseObject = new JSONObject(responseBody);
        return releaseObject.getString("tag_name");
    }
}