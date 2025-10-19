package com.gui;

import com.etl.ReadData;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

public class ChartPanel extends ContentPanel {
    private long[] times;
    private double[] prices;

    private double minPrice = Double.MAX_VALUE;
    private double maxPrice = Double.MIN_VALUE;
    private long minTime = Long.MAX_VALUE;
    private long maxTime = Long.MIN_VALUE;

    // The format for parsing the input data from your CSV
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public ChartPanel() {
        this.times = null;
        this.prices = null;
        setPreferredSize(new Dimension(800, 400));

        // Set a modern background and padding for the chart area
        setBackground(Color.WHITE);
        // Added extra padding on the right for labels (Top, Left, Bottom, Right)
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 60));
    }

    /**
     * Loads, filters, and processes a new symbol's data to be displayed on the chart.
     */
    public void openChart(ReadData reader, String symbol) {
        List<String[]> rows = reader.getFileData(symbol + ".csv");

        if (rows == null || rows.size() < 2) {
            times = null;
            prices = null;
            repaint();
            return;
        }

        // Use a TreeMap to automatically sort all data by time
        TreeMap<Long, Double> sortedData = new TreeMap<>();
        for (int i = 1; i < rows.size(); i++) { // skip header row
            try {
                long t = sdf.parse(rows.get(i)[1]).getTime();
                double p = Double.parseDouble(rows.get(i)[2]);
                sortedData.put(t, p);
            } catch (ParseException | NumberFormatException e) {
                // Skip any rows with invalid data
            }
        }

        if (sortedData.isEmpty()) {
            times = null;
            prices = null;
            repaint();
            return;
        }

        // --- FILTER DATA FOR THE LAST 3 DAYS ---
        long latestTime = sortedData.lastKey();
        long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000; // Use 'L' for long literal
        long cutoffTime = latestTime - threeDaysInMillis;

        // Use tailMap to get a new map containing only the last 3 days of data
        Map<Long, Double> lastThreeDaysData = sortedData.tailMap(cutoffTime);
        // --- END OF FILTERING ---

        // === MODIFIED LINE FOR SIMPLIFICATION ===
        // Instead of plotting one point per pixel, we target a fixed number of points
        // to create a much smoother and less cluttered line.
        int maxPoints = 200; // Target a fixed number of points for a smoother line
        // ========================================

        // Downsample the filtered data if it's too large for the screen width
        int dataSize = lastThreeDaysData.size();
        if (dataSize == 0) {
            times = null;
            prices = null;
            repaint();
            return;
        }

        int step = Math.max(1, dataSize / maxPoints);
        int finalSize = (dataSize + step - 1) / step;

        times = new long[finalSize];
        prices = new double[finalSize];

        // Reset min/max values before processing the new data set
        minTime = Long.MAX_VALUE;
        maxTime = Long.MIN_VALUE;
        minPrice = Double.MAX_VALUE;
        maxPrice = Double.MIN_VALUE;

        int index = 0;
        int count = 0;
        // Iterate over the filtered map to prepare data for rendering
        for (Map.Entry<Long, Double> entry : lastThreeDaysData.entrySet()) {
            if (count % step == 0) {
                long t = entry.getKey();
                double p = entry.getValue();

                if (index < times.length) {
                    times[index] = t;
                    prices[index] = p;
                    index++;
                }

                minTime = Math.min(minTime, t);
                maxTime = Math.max(maxTime, t);
                minPrice = Math.min(minPrice, p);
                maxPrice = Math.max(maxPrice, p);
            }
            count++;
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