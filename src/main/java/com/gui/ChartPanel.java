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
    private DatabaseManager dbRef; // set on first openChart call
    private String symbol;

    private final ChartCanvas canvas;
    private final TimeframeBar timeframeBar;
    private final OrderPanel orderPanel;

    public ChartPanel() {
        super();
        this.symbol = null;
        setLayout(new BorderLayout(0, 10));
        setOpaque(true);
        setBackground(GUIComponents.BG_DARK); // container color

        // chart canvas
        this.canvas = new ChartCanvas();
        add(canvas, BorderLayout.CENTER);

        // chart options + order panel
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        this.timeframeBar = new TimeframeBar((startMs, endMs) -> {
            if (dbRef != null && symbol != null) {
                openChart(dbRef, symbol, startMs, endMs, 400);
            }
        });
        south.add(timeframeBar);

        this.orderPanel = new OrderPanel();
        south.add(orderPanel);
        add(south, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(600, 300));
    }

    /**
     * load data for a symbol from database
     * timeframe default to 90 days
     * @param db
     * @param symbol
     */
    public void openChart(DatabaseManager db, String symbol) {
        this.dbRef = db;
        this.symbol = symbol;

        if (!timeframeBar.fireCurrentSelection()) {
            try {
                long latest = db.getLatestTimestamp(symbol);
                if (latest == 0L) { canvas.clear(symbol); return; }
                long ninetyDays = 90L * 24 * 60 * 60 * 1000L;
                long start = Math.max(0, latest - ninetyDays);
                openChart(db, symbol, start, latest, 400);
            } catch (Exception e) {
                canvas.clear(symbol);
            }
        }
    }

    /**
     * load data for a symbol from database and prep it for painting
     * overloaded to specify time frame
     *
     * @param db        DatabaseManager
     * @param symbol    e.g. "AAPL"
     * @param startMs   inclusive epoch millis
     * @param endMs     inclusive epoch millis
     * @param maxPoints cap plotted points to keep the line smooth (e.g. 200)
     */
    public void openChart(DatabaseManager db, String symbol, long startMs, long endMs, int maxPoints) {
        this.dbRef = db;
        this.symbol = symbol;
        canvas.loadFromDb(db, symbol, startMs, endMs, maxPoints);
    }

    // ===========
    // ChartCanvas
    // ===========
    private static final class ChartCanvas extends ContentPanel {
        private long[] times;
        private double[] prices;
        private String symbol;

        private double minPrice = Double.MAX_VALUE;
        private double maxPrice = Double.MIN_VALUE;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = Long.MIN_VALUE;

        ChartCanvas() {
            setPreferredSize(new Dimension(800, 400));
            setBackground(GUIComponents.BG_LIGHTER);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(20, 20, 20, 60)
            ));
        }

        void clear(String symbol) {
            this.symbol = symbol;
            this.times = null;
            this.prices = null;
            repaint();
        }

        void loadFromDb(DatabaseManager db, String symbol, long startMs, long endMs, int maxPoints) {
            this.symbol = symbol;
            try (ResultSet rs = db.getPrices(symbol, startMs, endMs)) {
                TreeMap<Long, Double> sorted = new TreeMap<>();
                while (rs.next()) {
                    long t = rs.getLong("timestamp");
                    double c = rs.getDouble("close");
                    sorted.put(t, c);
                }

                if (sorted.isEmpty()) { times = null; prices = null; repaint(); return; }

                int dataSize = sorted.size();
                int step = Math.max(1, dataSize / Math.max(1, maxPoints));
                int finalSize = (dataSize + step - 1) / step;

                times = new long[finalSize];
                prices = new double[finalSize];

                minTime = Long.MAX_VALUE; maxTime = Long.MIN_VALUE;
                minPrice = Double.MAX_VALUE; maxPrice = Double.MIN_VALUE;

                int i = 0, k = 0;
                for (Map.Entry<Long, Double> e : sorted.entrySet()) {
                    if (k++ % step != 0) continue;
                    if (i >= finalSize) break;
                    long t = e.getKey();
                    double p = e.getValue();
                    times[i] = t; prices[i] = p;
                    if (t < minTime) minTime = t; if (t > maxTime) maxTime = t;
                    if (p < minPrice) minPrice = p; if (p > maxPrice) maxPrice = p;
                    i++;
                }
            } catch (Exception ex) {
                times = null; prices = null;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (times == null || times.length < 2) {
                g.setColor(new Color(150, 150, 150));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g.getFontMetrics();
                String msg = (symbol == null) ? "Select a stock to view chart" : ("No data available for " + symbol);
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isPositive = prices[prices.length - 1] >= prices[0];
            Color lineColor = isPositive ? GUIComponents.ACCENT_GREEN : GUIComponents.ACCENT_RED;
            Color gradientStart = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 100);
            Color gradientEnd   = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0);
            Color labelColor = new Color(120, 120, 120);
            Font labelFont = new Font("Segoe UI", Font.PLAIN, 10);

            int w = getWidth();
            int h = getHeight();
            Insets in = getInsets();
            int drawWidth = w - in.left - in.right;
            int drawHeight = h - in.top - in.bottom;
            if (maxTime == minTime || maxPrice == minPrice) return;

            int n = times.length;
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            for (int i = 0; i < n; i++) {
                xPoints[i] = in.left + (int) ((times[i] - minTime) * drawWidth / (double) (maxTime - minTime));
                yPoints[i] = h - in.bottom - (int) ((prices[i] - minPrice) * drawHeight / (maxPrice - minPrice));
            }

            Path2D.Double path = new Path2D.Double();
            path.moveTo(xPoints[0], h - in.bottom);
            for (int i = 0; i < n; i++) path.lineTo(xPoints[i], yPoints[i]);
            path.lineTo(xPoints[n - 1], h - in.bottom);
            path.closePath();

            GradientPaint gp = new GradientPaint(0, in.top, gradientStart, 0, h - in.bottom, gradientEnd);
            g2.setPaint(gp);
            g2.fill(path);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolyline(xPoints, yPoints, n);

            g2.setColor(labelColor);
            g2.setFont(labelFont);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(String.format("%.0f", maxPrice), w - in.right + 5, in.top + fm.getAscent());
            g2.drawString(String.format("%.0f", minPrice), w - in.right + 5, h - in.bottom);

            SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, HH:mm");
            String startLabel = timeFormat.format(new Date(minTime));
            String endLabel = timeFormat.format(new Date(maxTime));
            g2.drawString(startLabel, in.left, h - in.bottom + fm.getAscent() + 5);
            g2.drawString(endLabel, w - in.right - fm.stringWidth(endLabel), h - in.bottom + fm.getAscent() + 5);
        }
    }
}