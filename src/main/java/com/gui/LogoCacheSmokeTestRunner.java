package com.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Small smoke-test runner for LogoCache. Runs without compiling project tests.
 */
public class LogoCacheSmokeTestRunner {
    public static void main(String[] args) throws Exception {
        int size = 8;
        // create a tiny in-memory PNG
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0,0,16,16);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        String dataUrl = "data:image/png;base64," + b64;

        LogoCache cache = new LogoCache(size);
        CountDownLatch latch = new CountDownLatch(1);
        final ImageIcon[] result = new ImageIcon[1];

        cache.load("TEST", dataUrl, size, size, icon -> {
            result[0] = icon;
            latch.countDown();
        });

        boolean ok = latch.await(5, TimeUnit.SECONDS);
        if (!ok) {
            System.err.println("LogoCache load timed out");
            System.exit(2);
        }

        ImageIcon ic = result[0];
        if (ic == null || ic.getIconWidth() != size || ic.getIconHeight() != size) {
            System.err.println("Unexpected icon result: " + ic);
            System.exit(3);
        }

        System.out.println("LogoCache smoke test OK — icon size=" + ic.getIconWidth());
        cache.shutdown();
    }
}
