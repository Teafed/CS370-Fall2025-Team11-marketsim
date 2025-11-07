// includes chart area and orders panel

package com.gui;

import com.etl.HistoricalService;
import com.market.DatabaseManager;
import com.accountmanager.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.sql.Date;
import java.sql.ResultSet;
import java.text.*;
import java.time.*;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChartPanel extends ContentPanel {
    private DatabaseManager dbRef; // set on first openChart call
    private String symbol;

    private final ChartCanvas canvas;
    private final TimeframeBar timeframeBar;

    private SwingWorker<?,?> currentWorker; // for backfilling

    /* called once in MainWindow; loads first symbol on watchlist */
    public ChartPanel(Account account) {
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

        this.timeframeBar = new TimeframeBar((startMs, endMs, multiplier, timespanToken) -> {
            if (dbRef != null && symbol != null) {
                HistoricalService.Timespan ts = HistoricalService.Timespan.DAY;
                openChart(dbRef, symbol, multiplier, ts, startMs, endMs, 400);
            }
        });
        south.add(timeframeBar);

        OrderPanel orderPanel = new OrderPanel(account);
        south.add(orderPanel);
        add(south, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(600, 300));
    }

    /* load data for a symbol from database; timeframe default to 90 days */
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
        HistoricalService.Timespan timespan = HistoricalService.Timespan.DAY;
        openChart(db, symbol, 1, timespan, startMs, endMs, maxPoints);
    }

    public void openChart(DatabaseManager db,
                          String symbol,
                          int multiplier,
                          HistoricalService.Timespan timespan,
                          long startMs, long endMs, int maxPoints) {
        this.dbRef = db;
        this.symbol = symbol;
        canvas.setLoading(true);

        final long now = System.currentTimeMillis();
        final long endMsClamped = Math.min(endMs, now);
        final long startMsClamped = Math.min(startMs, endMsClamped);

        // build requested range once
        HistoricalService svc = new HistoricalService(dbRef);
        LocalDate from = Instant.ofEpochMilli(startMsClamped).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate to = Instant.ofEpochMilli(endMsClamped).atZone(ZoneOffset.UTC).toLocalDate();
        HistoricalService.Range requested = new HistoricalService.Range(timespan, multiplier, from, to);
        System.out.printf("[ChartPanel] Requested %s %d/%s %s - %s%n", symbol, multiplier, timespan, from, to);

        // check if it's already covered
        HistoricalService.Range missing = null;
        try {
            missing = svc.ensureRange(symbol, requested);
        } catch (Exception ignore) {}

        if (missing == null) {
            try { canvas.loadFromDb(dbRef, symbol, startMsClamped, endMsClamped, maxPoints); }
            finally { canvas.setLoading(false); }
            return;
        }

        HistoricalService.Range finalMissing = missing;
        startBackfillWorker(() -> svc.backfillRange(symbol, finalMissing), () -> {
            try { canvas.loadFromDb(dbRef, symbol, startMsClamped, endMsClamped, maxPoints); }
            finally { canvas.setLoading(false); }
        });
    }

    // only one worker fetches historical data
    // TODO: if a backfill worker starts for a range and we switch to a new view,
    //       we still keep backfilling for prev view but new view gets no worker
    private void startBackfillWorker(java.util.concurrent.Callable<Integer> task,
                                     Runnable onDone) {
        if (currentWorker != null && !currentWorker.isDone()) currentWorker.cancel(true);
        currentWorker = new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return task.call(); }
            @Override protected void done() { onDone.run(); }
        };
        currentWorker.execute();
    }

    // ===========
    // ChartCanvas
    // ===========
    private static class ChartCanvas extends ContentPanel {
        private String symbol = null;
        private boolean loading = false;
        private TreeMap<Long, Double> prices = new TreeMap<>();
        private long minMs = 0;
        private long maxMs = 0;
        private double minPrice = 0;
        private double maxPrice = 0;

        ChartCanvas() {
            setPreferredSize(new Dimension(800, 400));
            setBackground(GUIComponents.BG_DARK);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(20, 20, 20, 60)
            ));
        }

        void clear(String symbol) {
            this.symbol = symbol;
            loading = false;
            prices.clear();
            repaint();
        }

        void setLoading(boolean loading) {
            this.loading = loading;
            repaint();
        }

        void setData(String symbol, TreeMap<Long, Double> prices) {
            this.symbol = symbol;
            this.prices = prices;
            loading = false;

            if (prices.isEmpty()) {
                minMs = maxMs = 0;
                minPrice = maxPrice = 0;
            } else {
                minMs = prices.firstKey();
                maxMs = prices.lastKey();
                minPrice = prices.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                maxPrice = prices.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (symbol == null) {
                g.setColor(new Color(150, 150, 150));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g.getFontMetrics();
                String msg = "Select a symbol";
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int plotWidth = getWidth() - 80; // 20 left, 60 right
            int plotHeight = getHeight() - 40; // 20 top, 20 bottom
            int plotX = 20;
            int plotY = 20;

            // Draw border
            g2.setColor(GUIComponents.BORDER_COLOR);
            g2.drawRect(plotX, plotY, plotWidth, plotHeight);

            if (loading) {
                g.setColor(new Color(150, 150, 150));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g.getFontMetrics();
                String msg = "Loading data for " + symbol + "...";
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            if (prices.isEmpty()) {
                g.setColor(new Color(150, 150, 150));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g.getFontMetrics();
                String msg = "No data for " + symbol;
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            // Draw price axis (right side)
            g2.setColor(GUIComponents.TEXT_SECONDARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            DecimalFormat priceFormat = new DecimalFormat("#,##0.00");

            for (int i = 0; i <= 5; i++) {
                double price = minPrice + (maxPrice - minPrice) * i / 5;
                int y = plotY + plotHeight - (int) (plotHeight * (price - minPrice) / (maxPrice - minPrice));
                g2.drawLine(plotX + plotWidth, y, plotX + plotWidth + 5, y); // Tick mark
                String priceStr = priceFormat.format(price);
                g2.drawString(priceStr, plotX + plotWidth + 10, y + 4);
            }

            // Draw time axis (bottom side)
            g2.setColor(GUIComponents.TEXT_SECONDARY);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            long timeRange = maxMs - minMs;
            for (int i = 0; i <= 5; i++) {
                long time = minMs + timeRange * i / 5;
                int x = plotX + (int) (plotWidth * (time - minMs) / timeRange);
                g2.drawLine(x, plotY + plotHeight, x, plotY + plotHeight + 5); // Tick mark
                String dateStr = dateFormat.format(new Date(time));
                g2.drawString(dateStr, x - g2.getFontMetrics().stringWidth(dateStr) / 2, plotY + plotHeight + 20);
            }

            // Draw price line
            g2.setColor(GUIComponents.ACCENT_COLOR);
            Path2D path = new Path2D.Double();
            boolean firstPoint = true;

            for (Map.Entry<Long, Double> entry : prices.entrySet()) {
                long time = entry.getKey();
                double price = entry.getValue();

                int x = plotX + (int) (plotWidth * (time - minMs) / timeRange);
                int y = plotY + plotHeight - (int) (plotHeight * (price - minPrice) / (maxPrice - minPrice));

                if (firstPoint) {
                    path.moveTo(x, y);
                    firstPoint = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            g2.draw(path);
        }

        /**
         * Load candle data for a symbol from the database and downsample to at most maxPoints
         * before calling setData.
         */
        void loadFromDb(DatabaseManager db, String symbol, long startMs, long endMs, int maxPoints) {
            if (db == null || symbol == null) { clear(symbol); return; }
            try (java.sql.ResultSet rs = db.getCandles(symbol, startMs, endMs)) {
                java.util.List<java.util.Map.Entry<Long, Double>> rows = new java.util.ArrayList<>();
                while (rs.next()) {
                    long ts = rs.getLong(1);
                    double close = rs.getDouble(5); // SELECT timestamp, open, high, low, close, volume
                    rows.add(new java.util.AbstractMap.SimpleEntry<>(ts, close));
                }

                if (rows.isEmpty()) {
                    setData(symbol, new TreeMap<>());
                    return;
                }

                // If points exceed maxPoints, downsample by picking roughly evenly spaced indices
                int total = rows.size();
                TreeMap<Long, Double> out = new TreeMap<>();
                if (total <= maxPoints || maxPoints <= 0) {
                    for (var e : rows) out.put(e.getKey(), e.getValue());
                } else {
                    double step = (double) total / (double) maxPoints;
                    for (int i = 0; i < maxPoints; i++) {
                        int idx = Math.min(total - 1, (int) Math.round(i * step));
                        var e = rows.get(idx);
                        out.put(e.getKey(), e.getValue());
                    }
                }
                setData(symbol, out);
            } catch (Exception ex) {
                // on error clear canvas for symbol
                clear(symbol);
            }
        }
    }
}