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

        // Candidate scales, from fine → coarse
        private static final TimeScale[] TIME_SCALES = new TimeScale[] {
                // show individual days, bold at month / year boundaries
                new TimeScale(ChronoUnit.DAYS,   1, ChronoUnit.MONTHS, 1),
                new TimeScale(ChronoUnit.DAYS,   2, ChronoUnit.MONTHS, 1),
                new TimeScale(ChronoUnit.DAYS,   5, ChronoUnit.MONTHS, 1),
                new TimeScale(ChronoUnit.DAYS,   7, ChronoUnit.MONTHS, 1),
                new TimeScale(ChronoUnit.DAYS,  14, ChronoUnit.MONTHS, 1),

                // show months, bold at years
                new TimeScale(ChronoUnit.MONTHS, 1, ChronoUnit.YEARS, 1),
                new TimeScale(ChronoUnit.MONTHS, 3, ChronoUnit.YEARS, 1),
                new TimeScale(ChronoUnit.MONTHS, 6, ChronoUnit.YEARS, 1),

                // show years, bold every Nth year
                new TimeScale(ChronoUnit.YEARS,  1, ChronoUnit.YEARS, 5),
                new TimeScale(ChronoUnit.YEARS,  2, ChronoUnit.YEARS, 10),
                new TimeScale(ChronoUnit.YEARS,  5, ChronoUnit.YEARS, 20)
        };


        // axis helpers
        private static final class TimeTick {
            final int index;
            final boolean major;
            final String majorLabel;
            final String minorLabel;
            final boolean drawGrid;

            TimeTick(int index, boolean major, String majorLabel, String minorLabel) {
                this(index, major, majorLabel, minorLabel, true);
            }

            TimeTick(int index, boolean major, String majorLabel, String minorLabel, boolean drawGrid) {
                this.index = index;
                this.major = major;
                this.majorLabel = majorLabel;
                this.minorLabel = minorLabel;
                this.drawGrid = drawGrid;
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

        private static final class TimeScale {
            final ChronoUnit unit;     // DAYS, MONTHS, YEARS
            final int step;            // spacing between ticks (1, 2, 3, 6, ...)
            final ChronoUnit majorUnit; // which unit should be considered "major" (for bold)
            final int majorStep;       // how often a major label occurs in that unit

            TimeScale(ChronoUnit unit, int step,
                      ChronoUnit majorUnit, int majorStep) {
                this.unit = unit;
                this.step = step;
                this.majorUnit = majorUnit;
                this.majorStep = majorStep;
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

            int n = times.length;
            if (n <= 0) {
                hoveredIndex = -1;
                return;
            }

            double xStep = (n > 1) ? (plotW / (double) (n - 1)) : 0.0;

            int nearestIndex = -1;
            int nearestDist = Integer.MAX_VALUE;

            // Find nearest data point by X
            for (int i = 0; i < n; i++) {
                int x = plotX + (int) Math.round(i * xStep);
                int dx = Math.abs(mousePos.x - x);
                if (dx < nearestDist) {
                    nearestDist = dx;
                    nearestIndex = i;
                }
            }

            hoveredIndex = nearestIndex;

            if (hoveredIndex >= 0) {
                hoveredPrice = prices[hoveredIndex];
                interpolatedTime = times[hoveredIndex];
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
            g2.drawLine(plotX, 0, plotX, h - in.bottom);
            g2.drawLine(0, h - in.bottom, w, h - in.bottom);

            int drawWidth = plotW;
            int drawHeight = plotH;

            int n = times.length;
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            double xStep = (n > 1) ? (drawWidth / (double) (n - 1)) : 0.0;

            for (int i = 0; i < n; i++) {
                xPoints[i] = plotX + (int) Math.round(i * xStep);
                yPoints[i] = h - in.bottom - (int) ((prices[i] - minPrice) * drawHeight / (maxPrice - minPrice));
            }

            java.util.List<TimeTick> timeTicks = computeTimeTicks(drawWidth, n);
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
                int x = plotX + (int) Math.round(tt.index * xStep);
                if (tt.drawGrid) {
                    g2.setColor(tt.major ? gridMajor : gridMinor);
                    g2.drawLine(x, in.top, x, h - in.bottom);
                }
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
                int x = plotX + (int) Math.round(tt.index * xStep);
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

                int hoveredX = plotX + (int) Math.round(hoveredIndex * xStep);
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

        private java.util.List<TimeTick> computeTimeTicks(int drawWidth, int n) {
            java.util.List<TimeTick> out = new ArrayList<>();
            if (times == null || n <= 0 || maxTime <= minTime) return out;

            ZoneId zone = ZoneId.of("UTC");
            LocalDate minDate = Instant.ofEpochMilli(minTime).atZone(zone).toLocalDate();
            LocalDate maxDate = Instant.ofEpochMilli(maxTime).atZone(zone).toLocalDate();
            long spanDays = ChronoUnit.DAYS.between(minDate, maxDate) + 1;

            // target number of visible labels based on pixels
            int targetLabels = Math.max(2, drawWidth / 80);

            // pick the best scale for this span+width
            TimeScale scale = chooseBestScale(minDate, maxDate, spanDays, targetLabels);

            // find the first calendar boundary ≥ minDate for this scale
            LocalDate firstTickDate = alignToScaleStart(minDate, scale);

            // pre-compute some helpers for index mapping
            java.util.List<Integer> usedIndices = new ArrayList<>();
            java.util.LinkedHashSet<Integer> uniqueIndices = new java.util.LinkedHashSet<>();

            for (LocalDate d = firstTickDate;
                 !d.isAfter(maxDate);
                 d = d.plus(scale.step, scale.unit)) {

                int idx = findNearestIndexForDate(d, zone);
                if (idx < 0 || idx >= n) continue;

                // avoid duplicate indices (e.g. weekends / gaps)
                if (!uniqueIndices.add(idx)) continue;

                boolean major = isMajorTickDate(d, scale);
                String majorLabel = null;
                String minorLabel = null;

                // Labeling strategy:
                //  - When showing DAYS: minor = day-of-month, major = month or year
                //  - When showing MONTHS: minor = month, major = year
                //  - When showing YEARS: minor = year, major = selected years only
                if (scale.unit == ChronoUnit.DAYS) {
                    // small-ish spans, show day numbers, bold month/year
                    if (major) {
                        if (d.getMonthValue() == 1 && d.getDayOfMonth() <= scale.step) {
                            // new year → bold year
                            majorLabel = String.valueOf(d.getYear());
                        } else {
                            // new month → bold month abbreviation
                            majorLabel = d.getMonth()
                                    .getDisplayName(java.time.format.TextStyle.SHORT, Locale.US);
                        }
                    } else {
                        minorLabel = String.valueOf(d.getDayOfMonth());
                    }
                } else if (scale.unit == ChronoUnit.MONTHS) {
                    // medium spans, show months, bold at year boundaries
                    if (major) {
                        majorLabel = String.valueOf(d.getYear());
                    } else {
                        minorLabel = d.getMonth()
                                .getDisplayName(java.time.format.TextStyle.SHORT, Locale.US);
                    }
                } else { // YEARS scale
                    int year = d.getYear();
                    if (major) {
                        majorLabel = String.valueOf(year);
                    } else {
                        minorLabel = String.valueOf(year);
                    }
                }

                out.add(new TimeTick(idx, major, majorLabel, minorLabel));
            }
            boolean hasAnyMajorLabel = false;
            for (TimeTick tt : out) {
                if (tt.majorLabel != null && !tt.majorLabel.isEmpty()) {
                    hasAnyMajorLabel = true;
                    break;
                }
            }

            // Special case: day-scale, but no month/year labels → inject month labels
            if (scale.unit == ChronoUnit.DAYS && !hasAnyMajorLabel) {
                // We’ll add one label per month in range, positioned at the nearest data index
                LocalDate firstMonthStart = LocalDate.of(minDate.getYear(), minDate.getMonth(), 1);
                if (firstMonthStart.isBefore(minDate)) {
                    firstMonthStart = firstMonthStart.plusMonths(1);
                }

                for (LocalDate m = firstMonthStart; !m.isAfter(maxDate); m = m.plusMonths(1)) {
                    int idx = findNearestIndexForDate(m, zone);
                    if (idx < 0 || idx >= n) continue;

                    String monthLabel = m.getMonth()
                            .getDisplayName(java.time.format.TextStyle.SHORT, Locale.US);

                    // Bold month label, but NO extra grid line
                    out.add(new TimeTick(idx, true, monthLabel, null, false));
                }
            }
            out.sort(java.util.Comparator.comparingInt(tt -> tt.index));
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

        private TimeScale chooseBestScale(LocalDate minDate,
                                          LocalDate maxDate,
                                          long spanDays,
                                          int targetLabels) {
            TimeScale best = TIME_SCALES[0];
            double bestError = Double.MAX_VALUE;

            for (TimeScale s : TIME_SCALES) {
                long count = estimateTickCount(minDate, maxDate, s);
                if (count <= 0) continue;

                double error = Math.abs(count - targetLabels);

                // prefer scales that don't wildly overshoot (e.g. > 2x target)
                if (count > targetLabels * 2 && error > bestError) {
                    continue;
                }

                if (error < bestError) {
                    bestError = error;
                    best = s;
                }
            }
            return best;
        }

        private long estimateTickCount(LocalDate minDate,
                                       LocalDate maxDate,
                                       TimeScale scale) {
            LocalDate first = alignToScaleStart(minDate, scale);
            if (first.isAfter(maxDate)) return 0;

            long count = 0;
            for (LocalDate d = first; !d.isAfter(maxDate); d = d.plus(scale.step, scale.unit)) {
                count++;
                if (count > 10_000) break; // safety guard
            }
            return count;
        }

        private LocalDate alignToScaleStart(LocalDate minDate, TimeScale scale) {
            LocalDate d;

            if (scale.unit == ChronoUnit.DAYS) {
                // start at the exact first trading day in the range
                d = minDate;
            } else if (scale.unit == ChronoUnit.MONTHS) {
                // start at the 1st of this month or a later aligned month
                d = LocalDate.of(minDate.getYear(), minDate.getMonth(), 1);
            } else { // YEARS
                d = LocalDate.of(minDate.getYear(), 1, 1);
            }

            // bump forward until we're within the range and aligned to step
            while (d.isBefore(minDate)) {
                d = d.plus(scale.step, scale.unit);
            }
            return d;
        }

        private int findNearestIndexForDate(LocalDate date, ZoneId zone) {
            if (times == null || times.length == 0) return -1;

            long target = date.atStartOfDay(zone).toInstant().toEpochMilli();

            int lo = 0;
            int hi = times.length - 1;

            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                long t = times[mid];

                if (t < target) {
                    lo = mid + 1;
                } else if (t > target) {
                    hi = mid - 1;
                } else {
                    return mid; // exact match
                }
            }

            // lo is first index with time >= target (or len)
            int idx1 = Math.min(lo, times.length - 1);
            int idx0 = Math.max(0, lo - 1);

            long dt1 = Math.abs(times[idx1] - target);
            long dt0 = Math.abs(times[idx0] - target);

            return (dt0 <= dt1) ? idx0 : idx1;
        }

        private boolean isMajorTickDate(LocalDate d, TimeScale scale) {
            if (scale.majorUnit == null) return false;

            if (scale.majorUnit == ChronoUnit.MONTHS) {
                // day-scale: month boundaries are major
                return d.getDayOfMonth() == 1;
            }

            if (scale.majorUnit == ChronoUnit.YEARS) {
                int year = d.getYear();

                if (scale.unit == ChronoUnit.DAYS) {
                    // day-scale: year boundary (Jan 1) is major
                    return d.getDayOfYear() == 1 && (year % scale.majorStep == 0);
                } else if (scale.unit == ChronoUnit.MONTHS) {
                    // month-scale: major at first month of year, with year step
                    return d.getMonthValue() == 1 && d.getDayOfMonth() == 1
                            && (year % scale.majorStep == 0);
                } else { // YEARS scale
                    return (year % scale.majorStep) == 0;
                }
            }

            return false;
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