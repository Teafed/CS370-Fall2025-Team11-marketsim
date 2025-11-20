package com.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

// move this somewhere to models package

/**
 * Manages caching of logo images. Supports memory and disk caching, and
 * asynchronous loading from URLs, file paths, or Base64 strings.
 */
public class LogoCache {
    private final ConcurrentMap<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private final ExecutorService ex;
    private final ImageIcon placeholder;
    private final Path cacheDir;

    /**
     * Initializes the LogoCache with a specific placeholder size. Creates the cache
     * directory if it doesn't exist.
     *
     * @param placeholderSize The size (width and height) of the placeholder image.
     */
    public LogoCache(int placeholderSize) {
        this.ex = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        BufferedImage ph = new BufferedImage(placeholderSize, placeholderSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ph.createGraphics();
        g.setColor(new Color(0xDDDDDD));
        g.fillRect(0, 0, placeholderSize, placeholderSize);
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, placeholderSize - 1, placeholderSize - 1);
        g.dispose();
        this.placeholder = new ImageIcon(ph);

        // Create cache directory for persistent storage
        this.cacheDir = Paths.get("data", "logo-cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            System.err.println("Failed to create logo cache directory: " + e.getMessage());
        }
    }

    /**
     * Returns the placeholder image.
     *
     * @return The placeholder ImageIcon.
     */
    public ImageIcon getPlaceholder() {
        return placeholder;
    }

    /**
     * Retrieves the image from the memory cache if available.
     *
     * @param key The key (symbol or URL) to look up.
     * @return The cached ImageIcon, or null if not found.
     */
    public ImageIcon getIfCached(String key) {
        if (key == null)
            return null;
        return cache.get(key);
    }

    /**
     * Loads a logo asynchronously. Checks memory cache, then disk cache, then
     * downloads if necessary.
     *
     * @param key      The symbol or stable key for the logo.
     * @param logoStr  The source of the logo (URL, file path, or data URI).
     * @param w        Desired width.
     * @param h        Desired height.
     * @param callback Callback to be executed on the EDT with the loaded image (or
     *                 placeholder).
     */
    public void load(String key, String logoStr, int w, int h, Consumer<ImageIcon> callback) {
        Objects.requireNonNull(callback);
        String cacheKey = logoStr != null ? logoStr : key;
        if (cacheKey == null) {
            SwingUtilities.invokeLater(() -> callback.accept(placeholder));
            return;
        }

        // Check memory cache first
        ImageIcon existing = cache.get(cacheKey);
        if (existing != null) {
            SwingUtilities.invokeLater(() -> callback.accept(existing));
            return;
        }

        // Check disk cache next
        Path cachedFile = getCachedFilePath(cacheKey);
        if (Files.exists(cachedFile)) {
            ex.submit(() -> {
                try {
                    BufferedImage img = ImageIO.read(cachedFile.toFile());
                    if (img != null) {
                        BufferedImage resized = resizeImage(img, w, h);
                        ImageIcon result = new ImageIcon(resized);
                        // Store in memory cache for faster subsequent access
                        cache.put(cacheKey, result);
                        cache.put(key, result);
                        SwingUtilities.invokeLater(() -> callback.accept(result));
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load cached logo from disk: " + e.getMessage());
                }
                // If disk cache failed, fall through to download
                downloadAndCache(key, logoStr, w, h, callback, cacheKey, cachedFile);
            });
            return;
        }

        // Not in memory or disk cache - download it
        ex.submit(() -> downloadAndCache(key, logoStr, w, h, callback, cacheKey, cachedFile));
    }

    /**
     * Downloads the image from the source and saves it to the disk cache.
     *
     * @param key        The symbol or stable key.
     * @param logoStr    The source of the logo.
     * @param w          Desired width.
     * @param h          Desired height.
     * @param callback   Callback to be executed.
     * @param cacheKey   The key used for caching.
     * @param cachedFile The path to the cached file on disk.
     */
    private void downloadAndCache(String key, String logoStr, int w, int h,
            Consumer<ImageIcon> callback, String cacheKey, Path cachedFile) {
        BufferedImage img = null;
        try {
            img = loadImageFromString(logoStr);
            if (img != null) {
                // Save to disk cache
                saveToDisk(img, cachedFile);
            }
        } catch (Exception e) {
            System.err.println("Failed to download logo: " + e.getMessage());
        }

        final ImageIcon result;
        if (img == null) {
            result = placeholder;
        } else {
            BufferedImage resized = resizeImage(img, w, h);
            result = new ImageIcon(resized);
            // Cache in memory with BOTH the URL and the symbol key
            cache.put(cacheKey, result);
            cache.put(key, result);
        }
        SwingUtilities.invokeLater(() -> callback.accept(result));
    }

    /**
     * Generates a safe file path for the cached image based on the key.
     *
     * @param key The key to generate the filename from.
     * @return The path to the cached file.
     */
    private Path getCachedFilePath(String key) {
        // Create a safe filename from the key using hash
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(key.getBytes("UTF-8"));
            StringBuilder filename = new StringBuilder();
            for (byte b : hash) {
                filename.append(String.format("%02x", b));
            }
            return cacheDir.resolve(filename.toString() + ".png");
        } catch (Exception e) {
            // Fallback to simple sanitization
            String safe = key.replaceAll("[^a-zA-Z0-9.-]", "_");
            if (safe.length() > 100)
                safe = safe.substring(0, 100);
            return cacheDir.resolve(safe + ".png");
        }
    }

    /**
     * Saves the given image to the specified path on disk.
     *
     * @param img  The image to save.
     * @param path The path to save the image to.
     */
    private void saveToDisk(BufferedImage img, Path path) {
        try {
            ImageIO.write(img, "png", path.toFile());
        } catch (IOException e) {
            System.err.println("Failed to save logo to disk cache: " + e.getMessage());
        }
    }

    /**
     * Parses the string to load an image from a URL, Base64 string, or file path.
     *
     * @param s The string representing the image source.
     * @return The loaded BufferedImage, or null if loading fails.
     * @throws IOException If an I/O error occurs.
     */
    private static BufferedImage loadImageFromString(String s) throws IOException {
        if (s == null)
            return null;
        if (s.startsWith("http://") || s.startsWith("https://")) {
            URL url = new URL(s);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Marketsim/1.0");
            try (InputStream in = conn.getInputStream()) {
                return ImageIO.read(in);
            } finally {
                conn.disconnect();
            }
        } else if (s.startsWith("data:")) {
            // data:[<mediatype>][;base64],<data>
            String[] parts = s.split(",", 2);
            if (parts.length == 2) {
                byte[] bytes = Base64.getDecoder().decode(parts[1]);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
        } else {
            File f = new File(s);
            if (f.exists()) {
                return ImageIO.read(f);
            }
        }
        return null;
    }

    /**
     * Resizes the image to the specified dimensions using high-quality rendering
     * hints.
     *
     * @param src The source image.
     * @param w   The target width.
     * @param h   The target height.
     * @return The resized BufferedImage.
     */
    private static BufferedImage resizeImage(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return dst;
    }

    /**
     * Shuts down the executor service used for asynchronous loading.
     */
    public void shutdown() {
        ex.shutdownNow();
    }
}