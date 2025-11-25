// includes chart area and orders panel

package com.gui;

import com.etl.HistoricalService;
import com.gui.tabs.OrderPanel;
import com.models.Database;
import com.models.ModelFacade;

import java.awt.geom.Path2D;
import java.sql.Date;
import java.sql.ResultSet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.*;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A panel that displays a stock chart and an order panel.
 * Handles fetching and displaying historical data, as well as user interactions
 * for timeframes and orders.
 */
public class ChartPanel extends ContentPanel {
    private ModelFacade model;
    private String symbol;

    private final ChartCanvas canvas;
    private final TimeframeBar timeframeBar;

    private JSplitPane split;
    private ContentPanel south;
    private OrderPanel orderPanel;
    private int lastDividerLocation = -1;

    private SwingWorker<?, ?> currentWorker; // for backfilling

    // Rate limiting for API calls: 60 calls per minute
    private final AtomicLong lastMinuteStart = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger callsThisMinute = new AtomicInteger(0);
    private static final int MAX_CALLS_PER_MINUTE = 60;

    /**
     * Constructs a new ChartPanel.
     *
     * @param model The ModelFacade instance.
     */
    public ChartPanel(ModelFacade model) {
        super();
        this.model = model;
        this.symbol = null;
        setLayout(new BorderLayout(0, 10));
        setOpaque(true);
        setBackground(GUIComponents.BG_DARK); // container color

        // chart canvas
        this.canvas = new ChartCanvas();

        // chart options + order panel
        south = new ContentPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        this.timeframeBar = new TimeframeBar((startMs, endMs, multiplier, timespanToken) -> {
            if (model != null && symbol != null) {
                HistoricalService.Timespan ts = HistoricalService.Timespan.DAY;
                openChart(symbol, multiplier, ts, startMs, endMs, 400);
            }
        });
        south.add(timeframeBar);

        orderPanel = new OrderPanel(model, () -> this.symbol,
                collapsed -> SwingUtilities.invokeLater(() -> adjustDivider(collapsed)));
        south.add(orderPanel);
        south.setMinimumSize(new Dimension(0, timeframeBar.getPreferredSize().height + orderPanel.getHeaderHeight()));

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, canvas, south);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(GUIComponents.BG_DARK);
        split.setDividerSize(0);
        split.setEnabled(false);
        split.setResizeWeight(0.75);

        canvas.setMinimumSize(new Dimension(0, 370));
        south.setPreferredSize(new Dimension(0, 300));

        add(split, BorderLayout.CENTER);
        setMinimumSize(new Dimension(600, 300));

        SwingUtilities.invokeLater(() -> setDividerHeight(300));

        // When the ChartPanel is resized, keep south at its last chosen height unless collapsed
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (split != null && split.isVisible() && !orderPanel.isCollapsed()) {
                    setDividerHeight(lastDividerLocation > 0 ? (getHeight() - lastDividerLocation) : 240);
                }
            }
        });

        // Start auto-refresh timer for real-time updates
        startAutoRefresh();
    }

    /**
     * Rate-limited API call checker
     * @return true if call is allowed, false if rate limit exceeded
     */
    private boolean canMakeApiCall() {
        long now = System.currentTimeMillis();
        long minuteStart = lastMinuteStart.get();

        // reset counter
        if (now - minuteStart >= 60000) {
            lastMinuteStart.set(now);
            callsThisMinute.set(0);
        }

        // check if we're under the limit
        if (callsThisMinute.get() < MAX_CALLS_PER_MINUTE) {
            callsThisMinute.incrementAndGet();
            return true;
        }

        return false;
    }

    /**
     * Start automatic chart refresh to check for new data
     */
    private void startAutoRefresh() {
        Timer refreshTimer = new Timer(5000, e -> { // Check every 5 seconds
            if (symbol != null && model != null) {
                refreshChartData();
            }
        });
        refreshTimer.start();
    }

    /**
     * Refresh chart data by checking for new database entries
     */
    private void refreshChartData() {
        if (symbol == null || !canMakeApiCall()) {
            return;
        }

        SwingWorker<Boolean, Void> refreshWorker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Check if there's new data in the database
                long latestTimestamp = model.getLatestTimestamp(symbol);
                long currentMaxTime = canvas.getMaxTime();

                // If there's new data, reload the chart
                return latestTimestamp > currentMaxTime;
            }

            @Override
            protected void done() {
                try {
                    Boolean hasNewData = get();
                    if (hasNewData != null && hasNewData) {
                        // Reload chart with current timeframe
                        if (!timeframeBar.fireCurrentSelection()) {
                            openChart(symbol);
                        }
                    }
                } catch (Exception ex) {
                    // Silently handle refresh errors
                }
            }
        };
        refreshWorker.execute();
    }

    private void adjustDivider(boolean collapsed) {
        if (split == null) return;

        // If expanding, restore previous divider location or reasonable default
        if (!collapsed) {
            int h = split.getHeight();
            if (lastDividerLocation <= 0 || lastDividerLocation >= h) {
                // default to ~75% chart
                split.setDividerLocation(0.75);
            } else {
                split.setDividerLocation(lastDividerLocation);
            }
            return;
        }

        // Collapsing: remember current location, then shrink south to just timeframe + header
        lastDividerLocation = split.getDividerLocation();

        // Ensure south minimum reflects the collapsed height
        int southHeight = timeframeBar.getPreferredSize().height + orderPanel.getHeaderHeight();
        south.setMinimumSize(new Dimension(0, southHeight));

        // After current layout pass, set divider to leave exactly southHeight visible
        SwingUtilities.invokeLater(() -> {
            int h = split.getHeight();
            int target = Math.max(0, h - southHeight);
            split.setDividerLocation(target);
        });
    }

    private void setDividerHeight(int southHeightPx) {
        if (split == null) return;
        int h = split.getHeight();
        if (h <= 0) return;
        int target = Math.max(0, h - southHeightPx);
        split.setDividerLocation(target);
        lastDividerLocation = target; // remember for resize handler
    }

    /**
     * Loads data for a symbol from the database and prepares it for painting.
     * Defaults to a 90-day timeframe.
     *
     * @param symbol The stock symbol (e.g., "AAPL").
     */
    public void openChart(String symbol) {
        this.symbol = symbol;
        if (orderPanel != null) orderPanel.refreshHistory();
        if (!timeframeBar.fireCurrentSelection()) {
            try {
                long latest = model.getLatestTimestamp(symbol);
                if (latest == 0L) {
                    canvas.clear(symbol);
                    return;
                }
                long ninetyDays = 90L * 24 * 60 * 60 * 1000L;
                long start = Math.max(0, latest - ninetyDays);
                openChart(symbol, start, latest, 400);
            } catch (Exception e) {
                canvas.clear(symbol);
            }
        }
    }

    /**
     * Loads data for a symbol from the database and prepares it for painting.
     * Overloaded to specify a custom time frame.
     *
     * @param symbol    The stock symbol (e.g., "AAPL").
     * @param startMs   The start time in epoch milliseconds (inclusive).
     * @param endMs     The end time in epoch milliseconds (inclusive).
     * @param maxPoints The maximum number of points to plot (for smoothing).
     */
    public void openChart(String symbol, long startMs, long endMs, int maxPoints) {
        HistoricalService.Timespan timespan = HistoricalService.Timespan.DAY;
        openChart(symbol, 1, timespan, startMs, endMs, maxPoints);
    }

    /**
     * Loads data for a symbol with full control over timeframe and resolution.
     *
     * @param symbol     The stock symbol.
     * @param multiplier The time unit multiplier.
     * @param timespan   The time unit (e.g., DAY, MINUTE).
     * @param startMs    The start time in epoch milliseconds.
     * @param endMs      The end time in epoch milliseconds.
     * @param maxPoints  The maximum number of points to plot.
     */
    public void openChart(String symbol,
            int multiplier,
            HistoricalService.Timespan timespan,
            long startMs, long endMs, int maxPoints) {
        this.symbol = symbol;
        canvas.setLoading(true);

        final long now = System.currentTimeMillis();
        final long endMsClamped = Math.min(endMs, now);
        final long startMsClamped = Math.min(startMs, endMsClamped);

        var req = new ModelFacade.Range(timespan, multiplier, startMsClamped, endMsClamped);
        ModelFacade.Range missing = null;
        try {
            missing = model.ensureRange(symbol, req);
        } catch (Exception ignore) { }

        Runnable loadAndPaint = () -> {
            try {
                var pts = model.loadCloses(symbol, startMsClamped, endMsClamped, maxPoints);
                canvas.loadPoints(symbol, pts);
            } catch (Exception ex) {
                canvas.clear(symbol);
            } finally {
                canvas.setLoading(false);
            }
        };

        if (missing == null) {
            loadAndPaint.run();
            return;
        }

        ModelFacade.Range finalMissing = missing;
        startBackfillWorker(() -> {
            model.backfillRange(symbol, finalMissing);
            return 0;
        }, loadAndPaint);
    }

    // only one worker fetches historical data
    private volatile int workerGeneration = 0;
    private void startBackfillWorker(java.util.concurrent.Callable<Integer> task,
                                     Runnable onDone) {
        if (currentWorker != null && !currentWorker.isDone()) currentWorker.cancel(true);

        final int thisGeneration = ++workerGeneration;

        currentWorker = new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return task.call();
            }
            @Override protected void done() {
                if (thisGeneration == workerGeneration) onDone.run();
            }
        };
        currentWorker.execute();
    }

    // ===========
    // ChartCanvas
    // ===========
    private static final class ChartCanvas extends ContentPanel {
        private long[] times;
        private double[] prices;
        private String symbol;
        private boolean loading = false;

        // Hover tooltip state
        private Point mousePos = null;
        private int hoveredIndex = -1;
        private double hoveredPrice = 0;
        private long interpolatedTime = 0;

        ChartCanvas() {
            setPreferredSize(new Dimension(800, 400));
            setBackground(GUIComponents.BG_LIGHTER);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(20, 60, 20, 20)
            ));

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePos = e.getPoint();
                    updateHoveredPoint();
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    mousePos = null;
                    hoveredIndex = -1;
                    repaint();
                }
            });
        }

        /**
         * Find interpolated data for any point along the line
         */
        private void updateHoveredPoint() {
            if (mousePos == null || times == null || times.length == 0) {
                hoveredIndex = -1;
                return;
            }

            Insets in = getInsets();
            int w = getWidth();
            int h = getHeight();
            int drawWidth = w - in.left - in.right;
            int drawHeight = h - in.top - in.bottom;

            // check if mouse is in bounds
            if (mousePos.x < in.left || mousePos.x > w - in.right ||
                    mousePos.y < in.top || mousePos.y > h - in.bottom) {
                hoveredIndex = -1;
                return;
            }

            double timeRatio = (mousePos.x - in.left) / (double) drawWidth;
            long hoveredTime = minTime + (long)(timeRatio * (maxTime - minTime));

            hoveredIndex = -1;
            for (int i = 0; i < times.length - 1; i++) {
                if (hoveredTime >= times[i] && hoveredTime <= times[i + 1]) {
                    // Interpolate between points i and i+1
                    double t = (hoveredTime - times[i]) / (double)(times[i + 1] - times[i]);
                    hoveredPrice = prices[i] + t * (prices[i + 1] - prices[i]);
                    hoveredTime = hoveredTime; // Use interpolated time
                    hoveredIndex = i; // Store index for reference
                    this.interpolatedTime = hoveredTime;
                    break;
                }
            }

            // edge case handling
            if (hoveredIndex == -1) {
                if (hoveredTime < times[0]) {
                    hoveredIndex = 0;
                    hoveredPrice = prices[0];
                    this.interpolatedTime = times[0];
                } else if (hoveredTime > times[times.length - 1]) {
                    hoveredIndex = times.length - 1;
                    hoveredPrice = prices[times.length - 1];
                    this.interpolatedTime = times[times.length - 1];
                }
            }
        }

        long getMaxTime() {
            return maxTime;
        }

        void loadPoints(String symbol, java.util.List<ModelFacade.CandlePoint> pts) {
            this.symbol = symbol;
            if (pts == null || pts.size() < 2) {
                times = null; prices = null; repaint(); return;
            }
            times = new long[pts.size()];
            prices = new double[pts.size()];
            for (int i = 0; i < pts.size(); i++) {
                times[i] = pts.get(i).t();
                prices[i] = pts.get(i).close();
            }
            recomputeBounds();
            repaint();
        }

        private long minTime, maxTime;
        private double minPrice, maxPrice;
        private void recomputeBounds() {
            minTime = Long.MAX_VALUE; maxTime = Long.MIN_VALUE;
            minPrice = Double.MAX_VALUE; maxPrice = Double.MIN_VALUE;
            for (int i = 0; i < times.length; i++) {
                long t = times[i]; double p = prices[i];
                if (t < minTime) minTime = t; if (t > maxTime) maxTime = t;
                if (p < minPrice) minPrice = p; if (p > maxPrice) maxPrice = p;
            }
        }

        void clear(String symbol) {
            this.symbol = symbol;
            this.times = null;
            this.prices = null;
            repaint();
        }

        void setLoading(boolean v) {
            loading = v;
            repaint();
        }

        void loadFromDb(Database db, String symbol, long startMs, long endMs, int maxPoints) {
            this.symbol = symbol;
            // use daily getCandles bc smaller timeframes aren't supported on free polygon
            try (ResultSet rs = db.getCandles(symbol, 1, "day", startMs, endMs)) {
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
                times = null;
                prices = null;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
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
            Color lineColor = isPositive ? GUIComponents.GREEN : GUIComponents.RED;
            Color gradientStart = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 100);
            Color gradientEnd = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0);
            Color labelColor = new Color(120, 120, 120);
            Font labelFont = new Font("Segoe UI", Font.PLAIN, 10);

            int w = getWidth();
            int h = getHeight();
            Insets in = getInsets();
            int drawWidth = w - in.left - in.right;
            int drawHeight = h - in.top - in.bottom;
            if (maxTime == minTime || maxPrice == minPrice)
                return;

            int n = times.length;
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            for (int i = 0; i < n; i++) {
                xPoints[i] = in.left + (int) ((times[i] - minTime) * drawWidth / (double) (maxTime - minTime));
                yPoints[i] = h - in.bottom - (int) ((prices[i] - minPrice) * drawHeight / (maxPrice - minPrice));
            }

            Path2D.Double path = new Path2D.Double();
            path.moveTo(xPoints[0], h - in.bottom);
            for (int i = 0; i < n; i++)
                path.lineTo(xPoints[i], yPoints[i]);
            path.lineTo(xPoints[n - 1], h - in.bottom);
            path.closePath();

            GradientPaint gp = new GradientPaint(0, in.top, gradientStart, 0, h - in.bottom, gradientEnd);
            g2.setPaint(gp);
            g2.fill(path);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolyline(xPoints, yPoints, n);

            // y-axis price labels
            g2.setColor(labelColor);
            g2.setFont(labelFont);
            FontMetrics fm = g2.getFontMetrics();
            String maxPriceStr = String.format("$%.2f", maxPrice);
            String minPriceStr = String.format("$%.2f", minPrice);
            g2.drawString(maxPriceStr, 5, in.top + fm.getAscent());
            g2.drawString(minPriceStr, 5, h - in.bottom);

            // x-axis time labels
            SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, HH:mm");
            timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String startLabel = timeFormat.format(new Date(minTime));
            String endLabel = timeFormat.format(new Date(maxTime));
            g2.drawString(startLabel, in.left, h - in.bottom + fm.getAscent() + 5);
            g2.drawString(endLabel, w - in.right - fm.stringWidth(endLabel), h - in.bottom + fm.getAscent() + 5);

            // draw hover tooltip
            if (hoveredIndex >= 0) {
                int hoveredX = in.left + (int) ((interpolatedTime - minTime) * drawWidth / (double) (maxTime - minTime));
                int hoveredY = h - in.bottom - (int) ((hoveredPrice - minPrice) * drawHeight / (maxPrice - minPrice));

                drawTooltip(g2, hoveredX, hoveredY, interpolatedTime, hoveredPrice);
            }

            if (loading) {
                g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                String msg = "Loading…";
                fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, (getHeight() + fm.getAscent()) / 2);
                g2.dispose();
            }
        }

        /**
         * Draw hover tooltip showing price and time
         */
        private void drawTooltip(Graphics2D g2, int x, int y, long time, double price) {
            // Format data
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateStr = dateFormat.format(new Date(time));
            String priceStr = String.format("$%.2f", price);

            // Tooltip styling
            Font tooltipFont = new Font("Segoe UI", Font.PLAIN, 11);
            g2.setFont(tooltipFont);
            FontMetrics fm = g2.getFontMetrics();

            int padding = 8;
            int lineHeight = fm.getHeight();
            int tooltipWidth = Math.max(fm.stringWidth(dateStr), fm.stringWidth(priceStr)) + padding * 2;
            int tooltipHeight = lineHeight * 2 + padding * 2;

            // Position tooltip (avoid edges)
            int tooltipX = x + 15;
            int tooltipY = y - tooltipHeight - 10;

            if (tooltipX + tooltipWidth > getWidth() - 10) {
                tooltipX = x - tooltipWidth - 15;
            }
            if (tooltipY < 10) {
                tooltipY = y + 20;
            }

            // Draw tooltip background
            g2.setColor(new Color(40, 40, 40, 240));
            g2.fillRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 8, 8);
            g2.setColor(new Color(100, 100, 100));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 8, 8);

            // Draw text
            g2.setColor(Color.WHITE);
            g2.drawString(priceStr, tooltipX + padding, tooltipY + padding + fm.getAscent());
            g2.setColor(new Color(180, 180, 180));
            g2.drawString(dateStr, tooltipX + padding, tooltipY + padding + lineHeight + fm.getAscent());

            // Draw dot at data point
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 4, y - 4, 8, 8);
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x - 4, y - 4, 8, 8);
        }
    }
}