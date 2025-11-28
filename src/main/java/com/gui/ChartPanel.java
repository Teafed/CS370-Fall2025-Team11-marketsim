// includes chart area and orders panel

package com.gui;

import com.etl.HistoricalService;
import com.gui.tabs.OrderPanel;
import com.models.ModelFacade;

import java.awt.geom.Path2D;
import java.sql.Date;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
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

        // cached bounds
        private long minTime, maxTime;
        private double minPrice, maxPrice;

        // axis helpers

        private static final class TimeTick {
            final long time;
            final boolean major;        // for grid intensity
            final String majorLabel;    // bold
            final String minorLabel;    // normal

            TimeTick(long time, boolean major, String majorLabel, String minorLabel) {
                this.time = time;
                this.major = major;
                this.majorLabel = majorLabel;
                this.minorLabel = minorLabel;
            }
        }

        private static final class PriceTick {
            final double value;
            final boolean major;

            PriceTick(double value, boolean major) {
                this.value = value;
                this.major = major;
            }
        }

        ChartCanvas() {
            setPreferredSize(new Dimension(800, 400));
            setBackground(GUIComponents.BG_LIGHTER);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(20, 60, 32, 20)
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

        private void recomputeBounds() {
            minTime = Long.MAX_VALUE; maxTime = Long.MIN_VALUE;
            minPrice = Double.MAX_VALUE; maxPrice = Double.MIN_VALUE;
            if (times == null || times.length == 0) return;
            for (int i = 0; i < times.length; i++) {
                long t = times[i]; double p = prices[i];
                if (t < minTime) minTime = t; if (t > maxTime) maxTime = t;
                if (p < minPrice) minPrice = p; if (p > maxPrice) maxPrice = p;
            }

            if (maxTime == minTime) {
                maxTime += 1_000L * 60L * 60L; // +1h
            }
            if (maxPrice == minPrice) {
                maxPrice = minPrice + 1.0;
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
            int plotX = in.left;
            int plotY = in.top;
            int plotW = w - in.left - in.right;
            int plotH = h - in.top - in.bottom;

            // Check if mouse is inside the plot area
            if (mousePos.x < plotX || mousePos.x > plotX + plotW ||
                mousePos.y < plotY || mousePos.y > plotY + plotH) {
                hoveredIndex = -1;
                return;
            }

            if (maxTime <= minTime) {
                hoveredIndex = -1;
                return;
            }

            int n = times.length;
            int nearestIndex = -1;
            int nearestDist = Integer.MAX_VALUE;

            // Compute the screen x for each candle, then find the closest
            for (int i = 0; i < n; i++) {
                int x = plotX + (int) ((times[i] - minTime) * plotW / (double) (maxTime - minTime));
                int dx = Math.abs(mousePos.x - x);
                if (dx < nearestDist) {
                    nearestDist = dx;
                    nearestIndex = i;
                }
            }

            hoveredIndex = nearestIndex;

            if (hoveredIndex >= 0) {
                hoveredPrice = prices[hoveredIndex];
                interpolatedTime = times[hoveredIndex]; // now actually the data point time
            }
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
            Color gradientEnd   = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0);
            Color labelColor    = new Color(190, 190, 190);
            Color gridMajor     = new Color(255, 255, 255, 35);
            Color gridMinor     = new Color(255, 255, 255, 18);
            Font  labelFont     = new Font("Segoe UI", Font.PLAIN, 12);
            Font  labelFontBold = labelFont.deriveFont(Font.BOLD);

            int w = getWidth();
            int h = getHeight();
            Insets in = getInsets();

            int plotX = in.left;
            int plotY = in.top;
            int plotW = w - in.left - in.right;
            int plotH = h - in.top - in.bottom;

            if (maxTime == minTime || maxPrice == minPrice)
                return;

            Color axisBg = GUIComponents.BG_LIGHTER;
            Color plotBg = GUIComponents.BG_LIGHTER;

            g2.setColor(getBackground());
            g2.fillRect(0, 0, w, h);
            g2.setColor(plotBg);
            g2.fillRect(plotX, plotY, plotW, plotH);
            g2.setColor(axisBg);
            g2.fillRect(0, 0, plotX, h);
            g2.fillRect(0, h - in.bottom, w, in.bottom);
            g2.setColor(gridMajor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(plotX, 0, plotX, h);
            g2.drawLine(0, h - in.bottom, w, h - in.bottom);

            int drawWidth = plotW;
            int drawHeight = plotH;

            int n = times.length;
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            for (int i = 0; i < n; i++) {
                xPoints[i] = in.left + (int) ((times[i] - minTime) * drawWidth / (double) (maxTime - minTime));
                yPoints[i] = h - in.bottom - (int) ((prices[i] - minPrice) * drawHeight / (maxPrice - minPrice));
            }

            // build time/price ticks for grid and axis
            java.util.List<TimeTick> timeTicks = computeTimeTicks(drawWidth, in);
            java.util.List<PriceTick> priceTicks = computePriceTicks(drawHeight);

            g2.setStroke(new BasicStroke(1f));

            // draw grid (price)
            for (PriceTick pt : priceTicks) {
                int y = h - in.bottom -
                        (int) ((pt.value - minPrice) * drawHeight / (maxPrice - minPrice));
                g2.setColor(pt.major ? gridMajor : gridMinor);

                g2.drawLine(in.left, y, w - in.right, y);
                // g2.drawLine(plotX, y, w - 1, y);
            }

            // draw grid (time)
            for (TimeTick tt : timeTicks) {
                int x = in.left +
                        (int) ((tt.time - minTime) * drawWidth / (double) (maxTime - minTime));
                g2.setColor(tt.major ? gridMajor : gridMinor);
                g2.drawLine(x, in.top, x, h - in.bottom);
                // g2.drawLine(x, 0, x, h - in.bottom);
            }

            // area under curve
            Path2D.Double path = new Path2D.Double();
            path.moveTo(xPoints[0], h - in.bottom);
            for (int i = 0; i < n; i++)
                path.lineTo(xPoints[i], yPoints[i]);
            path.lineTo(xPoints[n - 1], h - in.bottom);
            path.closePath();
            GradientPaint gp = new GradientPaint(0, in.top, gradientStart, 0, h - in.bottom, gradientEnd);
            g2.setPaint(gp);
            g2.fill(path);

            // price line
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolyline(xPoints, yPoints, n);

            // price axis labels
            g2.setFont(labelFont);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(labelColor);
            for (PriceTick pt : priceTicks) {
                int y = h - in.bottom -
                        (int) ((pt.value - minPrice) * drawHeight / (maxPrice - minPrice));
                String text = String.format("$%.2f", pt.value);
                int textY = y + fm.getAscent() / 2;
                g2.drawString(text, 8, textY);
            }

            // time axis labels
            int baseY = h - in.bottom + 2 + fm.getAscent();
            for (TimeTick tt : timeTicks) {
                int x = in.left +
                        (int) ((tt.time - minTime) * drawWidth / (double) (maxTime - minTime));
                String label = (tt.majorLabel != null) ? tt.majorLabel : tt.minorLabel;
                if (label == null || label.isEmpty()) continue;

                boolean isMajorLabel = (tt.majorLabel != null);
                g2.setFont(isMajorLabel ? labelFontBold : labelFont);
                FontMetrics fmLabel = g2.getFontMetrics();
                int tw = fmLabel.stringWidth(label);
                g2.setColor(labelColor);
                g2.drawString(label, x - tw / 2, baseY);
            }

            // draw hover tooltip
            if (hoveredIndex >= 0) {
                long t = times[hoveredIndex];
                double p = prices[hoveredIndex];

                int hoveredX = in.left + (int) ((t - minTime) * drawWidth / (double) (maxTime - minTime));
                int hoveredY = h - in.bottom - (int) ((p - minPrice) * drawHeight / (maxPrice - minPrice));

                drawTooltip(g2, hoveredX, hoveredY, t, p);
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

        private java.util.List<TimeTick> computeTimeTicks(int drawWidth, Insets in) {
            java.util.List<TimeTick> out = new ArrayList<>();
            if (maxTime <= minTime) return out;

            ZoneId zone = ZoneId.of("UTC");
            Instant minInst = Instant.ofEpochMilli(minTime);
            Instant maxInst = Instant.ofEpochMilli(maxTime);

            LocalDate startDate = minInst.atZone(zone).toLocalDate();
            LocalDate endDate = maxInst.atZone(zone).toLocalDate();
            long spanDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

            int approxLabelCount = Math.max(2, drawWidth / 80);
            int minPixelSpacing = 50;

            // long span -> months as minors, years as majors (at January)
            if (spanDays > 60) {
                LocalDate cursor = startDate.withDayOfMonth(1);
                if (cursor.isBefore(startDate)) cursor = cursor.plusMonths(1);

                long lastLabelX = Long.MIN_VALUE;
                while (!cursor.isAfter(endDate.plusMonths(1))) {
                    long t = cursor.atStartOfDay(zone).toInstant().toEpochMilli();
                    int x = in.left +
                            (int) ((t - minTime) * drawWidth / (double) (maxTime - minTime));

                    boolean isJanuary = cursor.getMonthValue() == 1;
                    boolean major = isJanuary;
                    String majorLabel = isJanuary ? String.valueOf(cursor.getYear()) : null;
                    String minorLabel = isJanuary
                            ? null
                            : cursor.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.US);

                    if (lastLabelX != Long.MIN_VALUE &&
                            x - lastLabelX < minPixelSpacing &&
                            !major) {
                        cursor = cursor.plusMonths(1);
                        continue;
                    }

                    out.add(new TimeTick(t, major, majorLabel, minorLabel));
                    lastLabelX = x;

                    cursor = cursor.plusMonths(1);
                    if (out.size() > approxLabelCount + 6) break; // safety
                }
            } else {
                // short span -> days as minors, month names as majors at 1st-of-month
                java.util.List<TimeTick> ticks = new ArrayList<>();

                long lastLabelX = Long.MIN_VALUE;

                LocalDate m = startDate.withDayOfMonth(1);
                if (m.isBefore(startDate)) m = m.plusMonths(1);
                while (!m.isAfter(endDate)) {
                    long t = m.atStartOfDay(zone).toInstant().toEpochMilli();
                    int x = in.left +
                            (int) ((t - minTime) * drawWidth / (double) (maxTime - minTime));

                    String majorLabel = m.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.US);
                    if (lastLabelX == Long.MIN_VALUE || x - lastLabelX >= minPixelSpacing) {
                        ticks.add(new TimeTick(t, true, majorLabel, null));
                        lastLabelX = x;
                    } else {
                        // if it's very close, still include it but overwrite spacing baseline
                        ticks.add(new TimeTick(t, true, majorLabel, null));
                        lastLabelX = x;
                    }

                    m = m.plusMonths(1);
                }

                int approxTicks = Math.max(3, drawWidth / 80);
                int stepDays = (int) Math.max(1, Math.round((double) spanDays / approxTicks));

                LocalDate d = startDate;
                while (!d.isAfter(endDate)) {
                    // skip 1st of month
                    if (d.getDayOfMonth() != 1) {
                        long t = d.atStartOfDay(zone).toInstant().toEpochMilli();
                        int x = in.left +
                                (int) ((t - minTime) * drawWidth / (double) (maxTime - minTime));

                        if (lastLabelX == Long.MIN_VALUE || x - lastLabelX >= minPixelSpacing) {
                            String minorLabel = String.valueOf(d.getDayOfMonth());
                            ticks.add(new TimeTick(t, false, null, minorLabel));
                            lastLabelX = x;
                        }
                    }
                    d = d.plusDays(stepDays);
                }

                // sort combined majors + minors by time
                ticks.sort(java.util.Comparator.comparingLong(tt -> tt.time));
                out = ticks;
            }

            return out;
        }

        private java.util.List<PriceTick> computePriceTicks(int drawHeight) {
            java.util.List<PriceTick> out = new ArrayList<>();
            double range = maxPrice - minPrice;
            if (range <= 0) return out;

            int approxTicks = Math.max(3, drawHeight / 60);
            double rawStep = range / approxTicks;
            double step = niceStep(rawStep);

            double start = Math.ceil(minPrice / step) * step;
            for (double v = start; v <= maxPrice + 1e-9; v += step) {
                boolean major = Math.abs(v / step - Math.round(v / step)) < 1e-6;
                out.add(new PriceTick(v, major));
            }
            return out;
        }

        private static double niceStep(double rawStep) {
            double exp = Math.floor(Math.log10(rawStep));
            double frac = rawStep / Math.pow(10, exp);
            double niceFrac;
            if (frac < 1.5) {
                niceFrac = 1.0;
            } else if (frac < 3) {
                niceFrac = 2.0;
            } else if (frac < 7) {
                niceFrac = 5.0;
            } else {
                niceFrac = 10.0;
            }
            return niceFrac * Math.pow(10, exp);
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