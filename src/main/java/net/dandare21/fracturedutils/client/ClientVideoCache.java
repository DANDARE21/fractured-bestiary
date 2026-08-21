package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientVideoCache {

    private static final String CACHE_SUBDIR = "cache/cinematics";
    private static final Pattern GOOGLE_DRIVE_ID_PATTERN = Pattern.compile("(?:id=|/d/)([a-zA-Z0-9_-]{25,})");

    public static File getCacheDirectory() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        File cacheDir = new File(gameDir, CACHE_SUBDIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    public static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    public static boolean isStreamUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("youtube.com") || lower.contains("youtu.be")
                || lower.contains("twitch.tv") || lower.contains("soundcloud.com")
                || lower.contains("medal.tv") || lower.contains("streamable.com")
                || lower.contains("bilibili.com") || lower.contains("tiktok.com");
    }

    public static String formatDownloadUrl(String url) {
        if (url == null) return url;
        if (url.contains("drive.google.com") || url.contains("drive.usercontent.google.com")) {
            Matcher matcher = GOOGLE_DRIVE_ID_PATTERN.matcher(url);
            if (matcher.find()) {
                String fileId = matcher.group(1);
                String directUrl = "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download&confirm=t";
                FracturedUtils.LOGGER.info("[VideoCache] Transformed Google Drive URL to direct endpoint: " + directUrl);
                return directUrl;
            }
        }
        return url;
    }

    public static File getNamedVideoFile(String customName) {
        if (customName == null) return null;
        String safeName = customName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        return new File(getCacheDirectory(), "named_" + safeName + ".mp4");
    }

    public static CompletableFuture<File> downloadNamedVideoAsync(String customName, String rawUrl) {
        File targetFile = getNamedVideoFile(customName);
        if (targetFile != null && targetFile.exists() && targetFile.length() > 2048 && isFileValidVideo(targetFile)) {
            FracturedUtils.LOGGER.info("[VideoCache] Pre-downloaded video for '" + customName + "' already exists: " + targetFile.getAbsolutePath());
            return CompletableFuture.completedFuture(targetFile);
        }

        final String downloadUrl = formatDownloadUrl(rawUrl);
        File cacheDir = getCacheDirectory();

        return CompletableFuture.supplyAsync(() -> {
            if (targetFile != null && targetFile.exists() && targetFile.length() > 2048 && isFileValidVideo(targetFile)) {
                return targetFile;
            }

            File tempFile = new File(cacheDir, "dl_" + UUID.randomUUID().toString().substring(0, 8) + ".tmp");
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .GET()
                        .build();

                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile.toPath()));
                if (response.statusCode() == 200) {
                    if (!isFileValidVideo(tempFile)) {
                        deleteCachedVideo(tempFile);
                        throw new RuntimeException("Downloaded content is invalid HTML markup, not video.");
                    }

                    if (targetFile == null) return tempFile;

                    if (targetFile.exists() && targetFile.length() > 2048 && isFileValidVideo(targetFile)) {
                        deleteCachedVideo(tempFile);
                        return targetFile;
                    }

                    try {
                        Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        FracturedUtils.LOGGER.info("[VideoCache] Pre-downloaded video '" + customName + "' saved to: " + targetFile.getAbsolutePath());
                        return targetFile;
                    } catch (Exception moveEx) {
                        if (targetFile.exists() && isFileValidVideo(targetFile)) {
                            deleteCachedVideo(tempFile);
                            return targetFile;
                        }
                        return tempFile;
                    }
                } else {
                    deleteCachedVideo(tempFile);
                    throw new RuntimeException("HTTP status " + response.statusCode());
                }
            } catch (Exception e) {
                deleteCachedVideo(tempFile);
                throw new CompletionException("Failed to pre-download video '" + customName + "'", e);
            }
        });
    }

    public static CompletableFuture<File> getVideoFileAsync(String rawUrl) {
        return getVideoFileAsync(rawUrl, null);
    }

    public static CompletableFuture<File> getVideoFileAsync(String rawUrl, String customName) {
        if (customName != null) {
            File namedFile = getNamedVideoFile(customName);
            if (namedFile != null && namedFile.exists() && namedFile.length() > 2048 && isFileValidVideo(namedFile)) {
                FracturedUtils.LOGGER.info("[VideoCache] Using pre-downloaded named video for '" + customName + "': " + namedFile.getAbsolutePath());
                return CompletableFuture.completedFuture(namedFile);
            }
        }

        final String downloadUrl = formatDownloadUrl(rawUrl);
        File cacheDir = getCacheDirectory();
        String fileHash = computeSha256(downloadUrl);
        File cachedFile = new File(cacheDir, fileHash + ".mp4");

        FracturedUtils.LOGGER.info("[VideoCache] Requesting video: rawUrl=" + rawUrl + " -> downloadUrl=" + downloadUrl);
        FracturedUtils.LOGGER.info("[VideoCache] Target cache path: " + cachedFile.getAbsolutePath() + " (exists=" + cachedFile.exists() + ", size=" + cachedFile.length() + " bytes)");

        if (cachedFile.exists() && cachedFile.length() > 2048 && isFileValidVideo(cachedFile)) {
            FracturedUtils.LOGGER.info("[VideoCache] Returning existing valid cached video file: " + cachedFile.getAbsolutePath());
            return CompletableFuture.completedFuture(cachedFile);
        }

        return CompletableFuture.supplyAsync(() -> {
            File tempFile = new File(cacheDir, "dl_" + UUID.randomUUID().toString().substring(0, 8) + ".tmp");
            FracturedUtils.LOGGER.info("[VideoCache] Starting HTTP download to temp file: " + tempFile.getAbsolutePath());
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .GET()
                        .build();

                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile.toPath()));
                FracturedUtils.LOGGER.info("[VideoCache] HTTP Response status: " + response.statusCode() + ", file bytes downloaded: " + tempFile.length());

                if (response.statusCode() == 200) {
                    if (!isFileValidVideo(tempFile)) {
                        deleteCachedVideo(tempFile);
                        FracturedUtils.LOGGER.error("[VideoCache] Downloaded file is invalid (contains HTML warning page instead of video bytes).");
                        throw new RuntimeException("Downloaded content is an HTML page (e.g. Google Drive virus scan warning or login page), not a valid video file.");
                    }

                    if (cachedFile.exists() && cachedFile.length() > 2048 && isFileValidVideo(cachedFile)) {
                        deleteCachedVideo(tempFile);
                        return cachedFile;
                    }

                    try {
                        Files.move(tempFile.toPath(), cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        FracturedUtils.LOGGER.info("[VideoCache] Video download complete. Saved to: " + cachedFile.getAbsolutePath());
                        return cachedFile;
                    } catch (Exception moveEx) {
                        if (cachedFile.exists() && isFileValidVideo(cachedFile)) {
                            deleteCachedVideo(tempFile);
                            return cachedFile;
                        }
                        return tempFile;
                    }
                } else {
                    deleteCachedVideo(tempFile);
                    FracturedUtils.LOGGER.error("[VideoCache] HTTP download failed with status code " + response.statusCode());
                    throw new RuntimeException("HTTP download failed with status code " + response.statusCode());
                }
            } catch (Exception e) {
                deleteCachedVideo(tempFile);
                FracturedUtils.LOGGER.error("[VideoCache] Exception during video download: " + e.getMessage(), e);
                throw new CompletionException("Failed to download video from URL: " + rawUrl, e);
            }
        });
    }

    public static void deleteCachedVideo(File file) {
        if (file != null && file.exists()) {
            try {
                boolean deleted = file.delete();
                FracturedUtils.LOGGER.info("[VideoCache] Deleted cached video file (" + file.getAbsolutePath() + "): " + deleted);
            } catch (Exception e) {
                FracturedUtils.LOGGER.warn("[VideoCache] Failed to delete video file: " + file.getAbsolutePath(), e);
            }
        }
    }

    private static boolean isFileValidVideo(File file) {
        if (!file.exists() || file.length() < 100) return false;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[512];
            int read = fis.read(header);
            if (read > 0) {
                String headStr = new String(header, 0, read, StandardCharsets.UTF_8).toLowerCase();
                if (headStr.contains("<!doctype html") || headStr.contains("<html") || headStr.contains("<head")) {
                    FracturedUtils.LOGGER.error("[VideoCache] Validation FAILED: File begins with HTML markup instead of video header.");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
