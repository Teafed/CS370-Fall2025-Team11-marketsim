// includes chart area and orders panel

package com.gui;

import com.market.DatabaseManager;

import java.awt.geom.Path2D;
import java.sql.Date;
import java.sql.ResultSet;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.Map;
import java.util.TreeMap;

public class ChartPanel extends ContentPanel {
    private long[] times;
    private double[] prices;
    private String symbol;

    private double minPrice = Double.MAX_VALUE;
    private double maxPrice = Double.MIN_VALUE;
    private long minTime = Long.MAX_VALUE;
    private long maxTime = Long.MIN_VALUE;

    public ChartPanel() {
        this.symbol = null;
        this.times = null;
        this.prices = null;
        setPreferredSize(new Dimension(800, 400));

        // Set a modern background and padding for the chart area
        setBackground(Color.WHITE);
        // Added extra padding on the right for labels (Top, Left, Bottom, Right)
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 60));
    }

    /**
     * load data for a symbol from database and prep it for painting
     *
     * example for how to call:
     * long now = System.currentTimeMillis();
     * long threeDays = 3L * 24 * 60 * 60 * 1000;
     * chartPanel.openChart(db, "AAPL", now - threeDays, now, 200);
     *
     * @param db        DatabaseManager
     * @param symbol    e.g. "AAPL"
     * @param startMs   inclusive epoch millis
     * @param endMs     inclusive epoch millis
     * @param maxPoints cap plotted points to keep the line smooth (e.g. 200)
     */
    public void openChart(DatabaseManager db, String symbol, long startMs, long endMs, int maxPoints) {
        this.symbol = symbol;

        try (ResultSet rs = db.getPrices(symbol, startMs, endMs)) {
            // Keep a sorted map of timestamp -> close (already sorted by query, but this is simple)
            TreeMap<Long, Double> sorted = new TreeMap<>();
            while (rs.next()) {
                long t = rs.getLong("timestamp");
                double c = rs.getDouble("close");
                sorted.put(t, c);
            }

            if (sorted.isEmpty()) {
                times = null;
                prices = null;
                repaint();
                return;
            }

            // downsample to maxPoints
            int dataSize = sorted.size();
            int step = Math.max(1, dataSize / Math.max(1, maxPoints));
            int finalSize = (dataSize + step - 1) / step;

            times = new long[finalSize];
            prices = new double[finalSize];

            // reset ranges
            minTime = Long.MAX_VALUE;
            maxTime = Long.MIN_VALUE;
            minPrice = Double.MAX_VALUE;
            maxPrice = Double.MIN_VALUE;

            int i = 0, k = 0;
            for (Map.Entry<Long, Double> e : sorted.entrySet()) {
                if (k++ % step != 0) continue;
                if (i >= finalSize) break;

                long t = e.getKey();
                double p = e.getValue();

                times[i] = t;
                prices[i] = p;

                if (t < minTime) minTime = t;
                if (t > maxTime) maxTime = t;
                if (p < minPrice) minPrice = p;
                if (p > maxPrice) maxPrice = p;
                i++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            times = null;
            prices = null;
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // If there's no data to show, display a simple message
        if (times == null || times.length < 2) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            String msg = "No data to display";
            g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Define modern colors and fonts
        Color chartColor = new Color(239, 83, 80); // Reddish color from example
        Color gradientStart = new Color(239, 83, 80, 180);
        Color gradientEnd = new Color(239, 83, 80, 0);
        Color labelColor = new Color(120, 120, 120);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 10);

        // Calculate drawing area based on the border padding
        int w = getWidth();
        int h = getHeight();
        int padTop = getInsets().top;
        int padLeft = getInsets().left;
        int padBottom = getInsets().bottom;
        int padRight = getInsets().right;
        int drawWidth = w - padLeft - padRight;
        int drawHeight = h - padTop - padBottom;

        if (maxTime == minTime || maxPrice == minPrice) return; // Avoid division by zero errors

        // Calculate all data point (x, y) coordinates
        int n = times.length;
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];
        for (int i = 0; i < n; i++) {
            xPoints[i] = padLeft + (int) ((times[i] - minTime) * drawWidth / (double) (maxTime - minTime));
            yPoints[i] = h - padBottom - (int) ((prices[i] - minPrice) * drawHeight / (maxPrice - minPrice));
        }

        // Create and draw the gradient fill area
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xPoints[0], h - padBottom);
        for (int i = 0; i < n; i++) {
            path.lineTo(xPoints[i], yPoints[i]);
        }
        path.lineTo(xPoints[n - 1], h - padBottom);
        path.closePath();

        GradientPaint gp = new GradientPaint(0, padTop, gradientStart, 0, h - padBottom, gradientEnd);
        g2.setPaint(gp);
        g2.fill(path);

        // Draw the main chart line on top of the fill
        g2.setColor(chartColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolyline(xPoints, yPoints, n);

        // Draw modern-style labels
        g2.setColor(labelColor);
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();

        // Y-Axis price labels on the right side
        g2.drawString(String.format("%.0f", maxPrice), w - padRight + 5, padTop + fm.getAscent());
        g2.drawString(String.format("%.0f", minPrice), w - padRight + 5, h - padBottom);

        // X-Axis time labels on the bottom
        SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, HH:mm");
        String startLabel = timeFormat.format(new Date(minTime));
        String endLabel = timeFormat.format(new Date(maxTime));

        g2.drawString(startLabel, padLeft, h - padBottom + fm.getAscent() + 5);
        g2.drawString(endLabel, w - padRight - fm.stringWidth(endLabel), h - padBottom + fm.getAscent() + 5);
    }
}