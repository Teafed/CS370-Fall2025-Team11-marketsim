package com.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class LogoCache {
    private final ConcurrentMap<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private final ExecutorService ex;
    private final ImageIcon placeholder;

    public LogoCache(int placeholderSize) {
        this.ex = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()/2));
        BufferedImage ph = new BufferedImage(placeholderSize, placeholderSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ph.createGraphics();
        g.setColor(new Color(0xDDDDDD));
        g.fillRect(0,0,placeholderSize,placeholderSize);
        g.setColor(Color.GRAY);
        g.drawRect(0,0,placeholderSize-1,placeholderSize-1);
        g.dispose();
        this.placeholder = new ImageIcon(ph);
    }

    public ImageIcon getPlaceholder() { return placeholder; }

    public ImageIcon getIfCached(String key) {
        if (key == null) return null;
        return cache.get(key);
    }

    /**
     * key: symbol (or any stable key)
     * logoStr: url | file path | data:base64
     * callback: invoked on EDT with ImageIcon (placeholder if failed)
     */
    public void load(String key, String logoStr, int w, int h, Consumer<ImageIcon> callback) {
        Objects.requireNonNull(callback);
        String cacheKey = logoStr != null ? logoStr : key;
        if (cacheKey == null) {
            SwingUtilities.invokeLater(() -> callback.accept(placeholder));
            return;
        }

        ImageIcon existing = cache.get(cacheKey);
        if (existing != null) {
            SwingUtilities.invokeLater(() -> callback.accept(existing));
            return;
        }

        // submit background task
        ex.submit(() -> {
            BufferedImage img = null;
            try {
                img = loadImageFromString(logoStr);
            } catch (Exception ignored) { }
            final ImageIcon result;
            if (img == null) {
                result = placeholder;
            } else {
                BufferedImage resized = resizeImage(img, w, h);
                result = new ImageIcon(resized);
                // Cache with BOTH the URL and the symbol key so we can retrieve by either
                cache.put(cacheKey, result);
                cache.put(key, result);  // Also cache with symbol key
            }
            SwingUtilities.invokeLater(() -> callback.accept(result));
        });
    }

    private static BufferedImage loadImageFromString(String s) throws IOException {
        if (s == null) return null;
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

    public void shutdown() {
        ex.shutdownNow();
    }
}